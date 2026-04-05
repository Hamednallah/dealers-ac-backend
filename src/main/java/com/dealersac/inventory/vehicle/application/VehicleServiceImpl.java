package com.dealersac.inventory.vehicle.application;

import com.dealersac.inventory.common.audit.Audited;
import com.dealersac.inventory.common.exception.CrossTenantAccessException;
import com.dealersac.inventory.common.exception.ResourceNotFoundException;
import com.dealersac.inventory.common.webhook.DomainEvent;
import com.dealersac.inventory.dealer.domain.Dealer;
import com.dealersac.inventory.dealer.repository.DealerRepository;
import com.dealersac.inventory.vehicle.domain.Vehicle;
import com.dealersac.inventory.vehicle.domain.VehicleStatus;
import com.dealersac.inventory.vehicle.dto.*;
import com.dealersac.inventory.vehicle.repository.VehicleRepository;
import com.dealersac.inventory.vehicle.specification.VehicleSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DealerRepository dealerRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    @Audited(action = "CREATE_VEHICLE", entityType = "Vehicle")
    @PreAuthorize("hasRole('TENANT_USER') or hasRole('GLOBAL_ADMIN')")
    public VehicleResponse create(VehicleRequest request, String tenantId) {
        // Verify dealer exists AND belongs to same tenant
        Dealer dealer = dealerRepository.findById(request.getDealerId())
                .orElseThrow(() -> new ResourceNotFoundException("Dealer", request.getDealerId()));

        if (!dealer.getTenantId().equals(tenantId)) {
            throw new CrossTenantAccessException();
        }

        Vehicle vehicle = Vehicle.builder()
                .tenantId(tenantId)
                .dealerId(request.getDealerId())
                .model(request.getModel())
                .price(request.getPrice())
                .status(request.getStatus() != null ? request.getStatus() : VehicleStatus.AVAILABLE)
                .build();

        vehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle created: {} in tenant: {}", vehicle.getId(), tenantId);
        return VehicleResponse.from(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('TENANT_USER') or hasRole('GLOBAL_ADMIN')")
    public VehicleResponse findById(UUID id, String tenantId) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));

        if (!vehicle.getTenantId().equals(tenantId)) {
            // Return 404 — don't confirm resource existence to other tenants
            throw new CrossTenantAccessException();
        }
        return VehicleResponse.from(vehicle);
    }

    @Override
    @Transactional
    @Audited(action = "RESERVE_VEHICLE", entityType = "Vehicle")
    @PreAuthorize("hasRole('TENANT_USER')")
    public VehicleResponse reserveForCheckout(java.util.UUID id, String tenantId) {
        Vehicle vehicle = vehicleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new IllegalStateException(
                    "Vehicle is not available for reservation. Current status: " + vehicle.getStatus());
        }

        vehicle.setStatus(VehicleStatus.RESERVED_PENDING_PAYMENT);
        vehicle.setReservationExpiresAt(java.time.Instant.now().plus(15, java.time.temporal.ChronoUnit.MINUTES));

        vehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle {} reserved for checkout until {}", vehicle.getId(), vehicle.getReservationExpiresAt());
        return VehicleResponse.from(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('TENANT_USER') or hasRole('GLOBAL_ADMIN')")
    public Page<VehicleResponse> findAll(VehicleFilter filter, Pageable pageable) {
        return vehicleRepository
                .findAll(VehicleSpecification.buildFrom(filter), pageable)
                .map(VehicleResponse::from);
    }

    @Override
    @Transactional
    @Audited(action = "UPDATE_VEHICLE", entityType = "Vehicle")
    @PreAuthorize("hasRole('TENANT_USER') or hasRole('GLOBAL_ADMIN')")
    public VehicleResponse update(UUID id, VehiclePatchRequest patch, String tenantId) {
        Vehicle vehicle = vehicleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));

        boolean wasAvailableOrReserved = vehicle.getStatus() == VehicleStatus.AVAILABLE
                || vehicle.getStatus() == VehicleStatus.RESERVED_PENDING_PAYMENT;

        // If trying to buy it directly, or complete a reservation
        if (patch.getStatus() == VehicleStatus.SOLD && !wasAvailableOrReserved) {
            throw new IllegalStateException("Vehicle cannot be sold from current status: " + vehicle.getStatus());
        }

        if (patch.getModel() != null)
            vehicle.setModel(patch.getModel());
        if (patch.getPrice() != null)
            vehicle.setPrice(patch.getPrice());
        if (patch.getStatus() != null)
            vehicle.setStatus(patch.getStatus());

        // Clear reservation if it gets sold
        if (vehicle.getStatus() == VehicleStatus.SOLD) {
            vehicle.setReservationExpiresAt(null);
        }

        vehicle = vehicleRepository.save(vehicle);

        // Fire VEHICLE_SOLD event when status transitions to SOLD
        if (wasAvailableOrReserved && vehicle.getStatus() == VehicleStatus.SOLD) {
            eventPublisher.publishEvent(new DomainEvent(this, "VEHICLE_SOLD", tenantId,
                    Map.of(
                            "vehicleId", vehicle.getId().toString(),
                            "model", vehicle.getModel(),
                            "price", vehicle.getPrice().toString(),
                            "dealerId", vehicle.getDealerId().toString())));
            log.info("Vehicle sold event fired: {}", vehicle.getId());
        }

        return VehicleResponse.from(vehicle);
    }

    @Override
    @Transactional
    @Audited(action = "DELETE_VEHICLE", entityType = "Vehicle")
    @PreAuthorize("hasRole('TENANT_USER') or hasRole('GLOBAL_ADMIN')")
    public void delete(UUID id, String tenantId) {
        Vehicle vehicle = vehicleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
        vehicleRepository.delete(vehicle);
        log.info("Vehicle deleted: {} from tenant: {}", id, tenantId);
    }
}
