package com.dealersac.inventory.vehicle;

import com.dealersac.inventory.common.exception.CrossTenantAccessException;
import com.dealersac.inventory.common.exception.ResourceNotFoundException;
import com.dealersac.inventory.dealer.domain.Dealer;
import com.dealersac.inventory.dealer.domain.SubscriptionType;
import com.dealersac.inventory.dealer.repository.DealerRepository;
import com.dealersac.inventory.vehicle.application.VehicleServiceImpl;
import com.dealersac.inventory.vehicle.domain.Vehicle;
import com.dealersac.inventory.vehicle.domain.VehicleStatus;
import com.dealersac.inventory.vehicle.dto.*;
import com.dealersac.inventory.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VehicleService Unit Tests")
class VehicleServiceTest {

    @Mock VehicleRepository       vehicleRepository;
    @Mock DealerRepository        dealerRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks VehicleServiceImpl vehicleService;

    private static final String TENANT_A = "tenant-alpha";
    private static final String TENANT_B = "tenant-beta";

    private Dealer dealer(String tenantId) {
        return Dealer.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .name("Test Dealer")
                .email("d@t.com")
                .subscriptionType(SubscriptionType.PREMIUM)
                .build();
    }

    private Vehicle vehicle(String tenantId, UUID dealerId) {
        return Vehicle.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .dealerId(dealerId)
                .model("Toyota Camry")
                .price(new BigDecimal("25000.00"))
                .status(VehicleStatus.AVAILABLE)
                .build();
    }

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("creates vehicle when dealer belongs to same tenant")
        void create_success() {
            Dealer d = dealer(TENANT_A);
            VehicleRequest req = new VehicleRequest();
            req.setDealerId(d.getId());
            req.setModel("Ford F-150");
            req.setPrice(new BigDecimal("45000"));
            req.setStatus(VehicleStatus.AVAILABLE);

            Vehicle saved = vehicle(TENANT_A, d.getId());
            when(dealerRepository.findById(d.getId())).thenReturn(Optional.of(d));
            when(vehicleRepository.save(any())).thenReturn(saved);

            VehicleResponse response = vehicleService.create(req, TENANT_A);
            assertThat(response.getTenantId()).isEqualTo(TENANT_A);
        }

        @Test
        @DisplayName("rejects vehicle creation when dealer belongs to different tenant")
        void create_dealerCrossTenant_throws() {
            Dealer d = dealer(TENANT_B); // dealer in Tenant B
            VehicleRequest req = new VehicleRequest();
            req.setDealerId(d.getId());
            req.setModel("BMW X5");
            req.setPrice(new BigDecimal("60000"));

            when(dealerRepository.findById(d.getId())).thenReturn(Optional.of(d));

            // Caller is Tenant A — dealer belongs to Tenant B
            assertThatThrownBy(() -> vehicleService.create(req, TENANT_A))
                    .isInstanceOf(com.dealersac.inventory.common.exception.IllegalArgumentException.class)
                    .hasMessageContaining("tenant");
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("returns vehicle for own tenant")
        void findById_ownTenant_success() {
            Vehicle v = vehicle(TENANT_A, UUID.randomUUID());
            when(vehicleRepository.findById(v.getId())).thenReturn(Optional.of(v));

            VehicleResponse response = vehicleService.findById(v.getId(), TENANT_A);
            assertThat(response.getId()).isEqualTo(v.getId());
        }

        @Test
        @DisplayName("throws CrossTenantAccessException for vehicle in other tenant")
        void findById_crossTenant_throws() {
            Vehicle v = vehicle(TENANT_A, UUID.randomUUID());
            when(vehicleRepository.findById(v.getId())).thenReturn(Optional.of(v));

            assertThatThrownBy(() -> vehicleService.findById(v.getId(), TENANT_B))
                    .isInstanceOf(CrossTenantAccessException.class);
        }
    }

    @Nested
    @DisplayName("update() — SOLD event")
    class UpdateTests {

        @Test
        @DisplayName("fires VEHICLE_SOLD event when status changes from AVAILABLE to SOLD")
        void update_soldTransition_firesEvent() {
            Vehicle v = vehicle(TENANT_A, UUID.randomUUID());
            when(vehicleRepository.findByIdAndTenantId(v.getId(), TENANT_A))
                    .thenReturn(Optional.of(v));
            when(vehicleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            VehiclePatchRequest patch = new VehiclePatchRequest();
            patch.setStatus(VehicleStatus.SOLD);

            vehicleService.update(v.getId(), patch, TENANT_A);

            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("does NOT fire VEHICLE_SOLD event when status unchanged")
        void update_noStatusChange_noEvent() {
            Vehicle v = vehicle(TENANT_A, UUID.randomUUID());
            when(vehicleRepository.findByIdAndTenantId(v.getId(), TENANT_A))
                    .thenReturn(Optional.of(v));
            when(vehicleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            VehiclePatchRequest patch = new VehiclePatchRequest();
            patch.setModel("Updated Model"); // only model changed

            vehicleService.update(v.getId(), patch, TENANT_A);

            verify(eventPublisher, never()).publishEvent(any());
        }
    }
}
