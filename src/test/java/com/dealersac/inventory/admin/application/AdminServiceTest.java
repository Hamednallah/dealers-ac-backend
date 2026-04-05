package com.dealersac.inventory.admin.application;

import com.dealersac.inventory.dealer.domain.SubscriptionType;
import com.dealersac.inventory.dealer.repository.DealerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService Unit Tests")
class AdminServiceTest {

    @Mock
    private DealerRepository dealerRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    @DisplayName("countDealersBySubscription aggregates results from repository")
    void countDealersBySubscription_success() {
        // Arrange
        Object[] basicCount = new Object[]{SubscriptionType.BASIC, 10L};
        Object[] premiumCount = new Object[]{SubscriptionType.PREMIUM, 5L};
        when(dealerRepository.countBySubscriptionType()).thenReturn(List.of(basicCount, premiumCount));

        // Act
        Map<String, Long> result = adminService.countDealersBySubscription();

        // Assert
        assertThat(result)
                .containsEntry("BASIC", 10L)
                .containsEntry("PREMIUM", 5L);
    }

    @Test
    @DisplayName("countDealersBySubscription returns zeros when repository returns empty")
    void countDealersBySubscription_emptyRepo() {
        // Arrange
        when(dealerRepository.countBySubscriptionType()).thenReturn(List.of());

        // Act
        Map<String, Long> result = adminService.countDealersBySubscription();

        // Assert
        assertThat(result)
                .containsEntry("BASIC", 0L)
                .containsEntry("PREMIUM", 0L);
    }
}
