package com.dealersac.inventory.vehicle.repository;

import com.dealersac.inventory.vehicle.domain.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID>,
        JpaSpecificationExecutor<Vehicle> {

    Optional<Vehicle> findByIdAndTenantId(UUID id, String tenantId);

    List<Vehicle> findAllByDealerIdAndTenantId(UUID dealerId, String tenantId);

    boolean existsByDealerIdAndTenantId(UUID dealerId, String tenantId);

    List<Vehicle> findByStatusAndReservationExpiresAtBefore(com.dealersac.inventory.vehicle.domain.VehicleStatus status, java.time.Instant expiry);
}
