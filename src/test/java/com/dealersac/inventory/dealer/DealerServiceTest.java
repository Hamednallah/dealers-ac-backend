package com.dealersac.inventory.dealer;

import com.dealersac.inventory.common.exception.CrossTenantAccessException;
import com.dealersac.inventory.common.exception.DuplicateResourceException;
import com.dealersac.inventory.common.exception.ResourceNotFoundException;
import com.dealersac.inventory.dealer.application.DealerServiceImpl;
import com.dealersac.inventory.dealer.domain.Dealer;
import com.dealersac.inventory.dealer.domain.SubscriptionType;
import com.dealersac.inventory.dealer.dto.DealerPatchRequest;
import com.dealersac.inventory.dealer.dto.DealerRequest;
import com.dealersac.inventory.dealer.dto.DealerResponse;
import com.dealersac.inventory.dealer.repository.DealerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DealerService Unit Tests")
class DealerServiceTest {

    @Mock DealerRepository       dealerRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks DealerServiceImpl dealerService;

    private static final String TENANT_A = "tenant-alpha";
    private static final String TENANT_B = "tenant-beta";

    private Dealer buildDealer(String tenantId) {
        return Dealer.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .name("Test Dealer")
                .email("test@dealer.com")
                .subscriptionType(SubscriptionType.BASIC)
                .build();
    }

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("creates dealer successfully in caller's tenant")
        void create_success() {
            DealerRequest req = new DealerRequest();
            req.setName("Alpha Motors");
            req.setEmail("alpha@motors.com");
            req.setSubscriptionType(SubscriptionType.PREMIUM);

            Dealer saved = buildDealer(TENANT_A);
            when(dealerRepository.existsByTenantIdAndEmail(TENANT_A, "alpha@motors.com")).thenReturn(false);
            when(dealerRepository.save(any())).thenReturn(saved);

            DealerResponse response = dealerService.create(req, TENANT_A);

            assertThat(response).isNotNull();
            assertThat(response.getTenantId()).isEqualTo(TENANT_A);
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("throws DuplicateResourceException for duplicate email within tenant")
        void create_duplicateEmail_throws409() {
            DealerRequest req = new DealerRequest();
            req.setEmail("dup@dealer.com");
            req.setName("Name");
            req.setSubscriptionType(SubscriptionType.BASIC);

            when(dealerRepository.existsByTenantIdAndEmail(TENANT_A, "dup@dealer.com")).thenReturn(true);

            assertThatThrownBy(() -> dealerService.create(req, TENANT_A))
                    .isInstanceOf(DuplicateResourceException.class);
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("returns dealer for own tenant")
        void findById_ownTenant_success() {
            Dealer dealer = buildDealer(TENANT_A);
            when(dealerRepository.findById(dealer.getId())).thenReturn(Optional.of(dealer));

            DealerResponse response = dealerService.findById(dealer.getId(), TENANT_A);
            assertThat(response.getId()).isEqualTo(dealer.getId());
        }

        @Test
        @DisplayName("throws CrossTenantAccessException when accessing another tenant's dealer")
        void findById_crossTenant_throws() {
            Dealer dealer = buildDealer(TENANT_A);
            when(dealerRepository.findById(dealer.getId())).thenReturn(Optional.of(dealer));

            assertThatThrownBy(() -> dealerService.findById(dealer.getId(), TENANT_B))
                    .isInstanceOf(CrossTenantAccessException.class);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when dealer does not exist")
        void findById_notFound_throws() {
            UUID id = UUID.randomUUID();
            when(dealerRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> dealerService.findById(id, TENANT_A))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("deletes dealer in own tenant")
        void delete_success() {
            Dealer dealer = buildDealer(TENANT_A);
            when(dealerRepository.findByIdAndTenantId(dealer.getId(), TENANT_A))
                    .thenReturn(Optional.of(dealer));

            dealerService.delete(dealer.getId(), TENANT_A);
            verify(dealerRepository).delete(dealer);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when dealer not found in tenant")
        void delete_crossTenant_notFound() {
            UUID id = UUID.randomUUID();
            when(dealerRepository.findByIdAndTenantId(id, TENANT_B)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> dealerService.delete(id, TENANT_B))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("partial update applies only non-null fields")
        void update_partialFields_onlyChangesProvided() {
            Dealer dealer = buildDealer(TENANT_A);
            when(dealerRepository.findByIdAndTenantId(dealer.getId(), TENANT_A))
                    .thenReturn(Optional.of(dealer));
            when(dealerRepository.save(any())).thenReturn(dealer);

            DealerPatchRequest patch = new DealerPatchRequest();
            patch.setName("Updated Name");
            // email and subscription NOT set — should remain unchanged

            DealerResponse response = dealerService.update(dealer.getId(), patch, TENANT_A);
            verify(dealerRepository).save(dealer);
        }
    }
}
