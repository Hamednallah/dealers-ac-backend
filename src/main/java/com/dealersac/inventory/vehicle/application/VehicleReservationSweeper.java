package com.dealersac.inventory.vehicle.application;

import com.dealersac.inventory.vehicle.domain.Vehicle;
import com.dealersac.inventory.vehicle.domain.VehicleStatus;
import com.dealersac.inventory.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleReservationSweeper {

    private final VehicleRepository vehicleRepository;

    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void evictExpiredReservations() {
        Instant now = Instant.now();
        List<Vehicle> expiredVehicles = vehicleRepository
                .findByStatusAndReservationExpiresAtBefore(VehicleStatus.RESERVED_PENDING_PAYMENT, now);

        if (!expiredVehicles.isEmpty()) {
            log.info("Found {} expired reservations. Reverting to AVAILABLE.", expiredVehicles.size());
            
            for (Vehicle vehicle : expiredVehicles) {
                vehicle.setStatus(VehicleStatus.AVAILABLE);
                vehicle.setReservationExpiresAt(null);
            }
            
            vehicleRepository.saveAll(expiredVehicles);
        }
    }
}
