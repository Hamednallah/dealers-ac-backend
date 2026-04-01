package com.dealersac.inventory.integration;

import com.dealersac.inventory.dealer.domain.Dealer;
import com.dealersac.inventory.dealer.domain.SubscriptionType;
import com.dealersac.inventory.dealer.repository.DealerRepository;
import com.dealersac.inventory.vehicle.application.VehicleService;
import com.dealersac.inventory.vehicle.domain.Vehicle;
import com.dealersac.inventory.vehicle.domain.VehicleStatus;
import com.dealersac.inventory.vehicle.dto.VehicleResponse;
import com.dealersac.inventory.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Integration Tests — Vehicle Checkout Concurrency")
class VehicleConcurrencyIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("dealersac_test_concurrency")
                    .withUsername("test")
                    .withPassword("test");

    @Autowired private VehicleService vehicleService;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private DealerRepository dealerRepository;

    private static final String TENANT_ID = "tenant-concurrency";
    private UUID testVehicleId;

    @BeforeEach
    void setup() {
        vehicleRepository.deleteAll();
        dealerRepository.deleteAll();

        Dealer dealer = Dealer.builder()
                .tenantId(TENANT_ID)
                .name("Concurrency Motors")
                .email("concurrency@motors.com")
                .subscriptionType(SubscriptionType.BASIC)
                .build();
        dealer = dealerRepository.save(dealer);

        Vehicle vehicle = Vehicle.builder()
                .tenantId(TENANT_ID)
                .dealerId(dealer.getId())
                .model("Model S")
                .price(BigDecimal.valueOf(50000))
                .status(VehicleStatus.AVAILABLE)
                .build();
        vehicle = vehicleRepository.save(vehicle);
        testVehicleId = vehicle.getId();
    }

    @Test
    @DisplayName("Concurrent reservations should allow only ONE to succeed")
    void reserveForCheckout_concurrencyTest() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                try {
                    latch.await(); // Wait until all threads are ready
                    vehicleService.reserveForCheckout(testVehicleId, TENANT_ID);
                    successes.incrementAndGet();
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Unleash the threads
        latch.countDown();
        doneLatch.await(); // wait for all threads to finish

        executorService.shutdown();

        // One success
        assertEquals(1, successes.get(), "Exactly one thread should succeed");
        // Nine failures (due to pessimistic/optimistic lock handling)
        assertEquals(9, failures.get(), "Other threads should fail with locking or state exceptions");

        // Verify database state directly
        Vehicle dbVehicle = vehicleRepository.findById(testVehicleId).orElseThrow();
        assertEquals(VehicleStatus.RESERVED_PENDING_PAYMENT, dbVehicle.getStatus());
        assertEquals(1, dbVehicle.getVersion()); // Version was updated exactly once
        assertNotNull(dbVehicle.getReservationExpiresAt());
    }
}
