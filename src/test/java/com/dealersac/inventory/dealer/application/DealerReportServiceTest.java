package com.dealersac.inventory.dealer.application;

import com.dealersac.inventory.common.exception.CrossTenantAccessException;
import com.dealersac.inventory.common.exception.ResourceNotFoundException;
import com.dealersac.inventory.dealer.domain.Dealer;
import com.dealersac.inventory.dealer.domain.SubscriptionType;
import com.dealersac.inventory.dealer.repository.DealerRepository;
import com.dealersac.inventory.vehicle.domain.Vehicle;
import com.dealersac.inventory.vehicle.domain.VehicleStatus;
import com.dealersac.inventory.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests — Dealer Report Service")
class DealerReportServiceTest {

    @Mock
    private DealerRepository dealerRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private DealerReportService reportService;

    @Test
    @DisplayName("Generate report — success")
    void generateReport_success() throws IOException {
        UUID dealerId = UUID.randomUUID();
        String tenantId = "tenant-1";
        
        Dealer dealer = Dealer.builder()
                .id(dealerId)
                .tenantId(tenantId)
                .name("Alpha Motors")
                .email("alpha@motors.com")
                .subscriptionType(SubscriptionType.PREMIUM)
                .build();

        Vehicle v1 = Vehicle.builder()
                .model("Model S")
                .price(BigDecimal.valueOf(80000))
                .status(VehicleStatus.AVAILABLE)
                .build();

        when(dealerRepository.findById(dealerId)).thenReturn(Optional.of(dealer));
        when(vehicleRepository.findAllByDealerIdAndTenantId(dealerId, tenantId)).thenReturn(List.of(v1));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        reportService.generateReport(dealerId, tenantId, out);

        assertTrue(out.size() > 0, "PDF should not be empty");
        verify(dealerRepository).findById(dealerId);
        verify(vehicleRepository).findAllByDealerIdAndTenantId(dealerId, tenantId);
    }

    @Test
    @DisplayName("Generate report — dealer not found throws ResourceNotFoundException")
    void generateReport_dealerNotFound_throws() {
        UUID dealerId = UUID.randomUUID();
        when(dealerRepository.findById(dealerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
                () -> reportService.generateReport(dealerId, "tenant-1", new ByteArrayOutputStream()));
    }

    @Test
    @DisplayName("Generate report — cross tenant access throws CrossTenantAccessException")
    void generateReport_crossTenant_throws() {
        UUID dealerId = UUID.randomUUID();
        Dealer dealer = Dealer.builder()
                .id(dealerId)
                .tenantId("tenant-A")
                .build();

        when(dealerRepository.findById(dealerId)).thenReturn(Optional.of(dealer));

        assertThrows(CrossTenantAccessException.class, 
                () -> reportService.generateReport(dealerId, "tenant-B", new ByteArrayOutputStream()));
    }
}
