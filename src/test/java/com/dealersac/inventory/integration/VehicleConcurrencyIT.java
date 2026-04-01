package com.dealersac.inventory.integration;

import com.dealersac.inventory.dealer.domain.Dealer;
import com.dealersac.inventory.dealer.domain.SubscriptionType;
import com.dealersac.inventory.dealer.repository.DealerRepository;
import com.dealersac.inventory.vehicle.application.VehicleService;
import com.dealersac.inventory.vehicle.domain.Vehicle;
import com.dealersac.inventory.vehicle.domain.VehicleStatus;
import com.dealersac.inventory.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency Integration Test — Checkout Reservation Race Condition
 *
 * WHAT WE ARE TESTING:
 * 10 threads simultaneously try to reserve the same vehicle.
 * Due to @Version (Optimistic Locking), only ONE should succeed.
 * ALL others must fail with either:
 *   - IllegalArgumentException (status != AVAILABLE after first write)
 *   - ObjectOptimisticLockingFailureException (JPA version mismatch)
 *
 * WHY WE NEED SECURITY CONTEXT PER THREAD:
 * @PreAuthorize runs on the calling thread via Spring AOP proxy.
 * New threads spawned by ExecutorService don't inherit the parent
 * SecurityContext. Each thread must set its own SecurityContext.
 */
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
    @DisplayName("Concurrent reservations — exactly ONE winner, all others fail")
    void reserveForCheckout_concurrencyTest() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);    // holds threads until we fire
        CountDownLatch doneLatch  = new CountDownLatch(numberOfThreads);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures  = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                // Each thread sets its own SecurityContext so @PreAuthorize passes
                SecurityContext ctx = SecurityContextHolder.createEmptyContext();
                ctx.setAuthentication(new UsernamePasswordAuthenticationToken(
                        "test-user", null,
                        List.of(new SimpleGrantedAuthority("ROLE_TENANT_USER"))
                ));
                SecurityContextHolder.setContext(ctx);

                try {
                    startLatch.await(); // Block until all threads are ready to fire simultaneously
                    vehicleService.reserveForCheckout(testVehicleId, TENANT_ID);
                    successes.incrementAndGet();
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    SecurityContextHolder.clearContext(); // Always clean up ThreadLocal
                    doneLatch.countDown();
                }
            });
        }

        // Release all threads at once — maximum contention
        startLatch.countDown();
        doneLatch.await();
        executorService.shutdown();

        // Only ONE thread should have successfully reserved the vehicle
        assertEquals(1, successes.get(), "Exactly one thread should succeed");
        assertEquals(9, failures.get(),  "All other threads should fail");

        // Verify final database state
        Vehicle dbVehicle = vehicleRepository.findById(testVehicleId).orElseThrow();
        assertEquals(VehicleStatus.RESERVED_PENDING_PAYMENT, dbVehicle.getStatus());
        assertEquals(1, dbVehicle.getVersion(), "Version should have been bumped exactly once");
        assertNotNull(dbVehicle.getReservationExpiresAt());
    }
}
