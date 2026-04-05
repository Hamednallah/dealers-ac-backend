package com.dealersac.inventory.dealer.application;

import com.dealersac.inventory.dealer.domain.Dealer;
import com.dealersac.inventory.dealer.domain.SubscriptionType;
import com.dealersac.inventory.dealer.repository.DealerRepository;
import com.dealersac.inventory.dealer.dto.DealerPatchRequest;
import com.dealersac.inventory.dealer.dto.DealerRequest;
import com.dealersac.inventory.dealer.dto.DealerResponse;
import com.dealersac.inventory.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Unit Tests — Dealer Service")
class DealerServiceTest {

    @Mock
    private DealerRepository dealerRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DealerServiceImpl dealerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Create dealer — saves and returns response")
    void create_savesDealer() {
        DealerRequest request = new DealerRequest();
        request.setName("Test Dealer");
        request.setEmail("test@email.com");
        request.setSubscriptionType(SubscriptionType.BASIC);

        Dealer savedDealer = Dealer.builder()
                .id(UUID.randomUUID())
                .name("Test Dealer")
                .email("test@email.com")
                .subscriptionType(SubscriptionType.BASIC)
                .tenantId("tenant-1")
                .build();

        when(dealerRepository.save(any(Dealer.class))).thenReturn(savedDealer);

        DealerResponse result = dealerService.create(request, "tenant-1");

        assertNotNull(result.getId());
        assertEquals("Test Dealer", result.getName());
        verify(dealerRepository).save(any(Dealer.class));
    }

    @Test
    @DisplayName("Find by ID — returns dealer if found")
    void findById_returnsDealer() {
        UUID id = UUID.randomUUID();
        Dealer dealer = Dealer.builder()
                .id(id)
                .name("Alpha")
                .tenantId("tenant-1")
                .build();

        when(dealerRepository.findById(id)).thenReturn(Optional.of(dealer));

        DealerResponse result = dealerService.findById(id, "tenant-1");

        assertEquals("Alpha", result.getName());
    }

    @Test
    @DisplayName("Find by ID — throws exception if not found")
    void findById_throwsIfNotFound() {
        UUID id = UUID.randomUUID();
        when(dealerRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> dealerService.findById(id, "tenant-1"));
    }

    @Test
    @DisplayName("Find all — returns paginated results")
    void findAll_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Dealer dealer = Dealer.builder().name("Test").build();
        Page<Dealer> page = new PageImpl<>(Collections.singletonList(dealer));

        when(dealerRepository.findAllByTenantId("tenant-1", pageable)).thenReturn(page);

        Page<DealerResponse> result = dealerService.findAll("tenant-1", pageable);

        assertEquals(1, result.getContent().size());
        assertEquals("Test", result.getContent().get(0).getName());
    }

    @Test
    @DisplayName("Update dealer — modifies and saves")
    void update_modifiesExisting() {
        UUID id = UUID.randomUUID();
        Dealer dealer = Dealer.builder().id(id).name("Old").tenantId("tenant-1").build();
        DealerPatchRequest patch = new DealerPatchRequest();
        patch.setName("New Name");

        when(dealerRepository.findByIdAndTenantId(id, "tenant-1")).thenReturn(Optional.of(dealer));
        when(dealerRepository.save(any(Dealer.class))).thenReturn(dealer);

        dealerService.update(id, patch, "tenant-1");

        assertEquals("New Name", dealer.getName());
        verify(dealerRepository).save(dealer);
    }

    @Test
    @DisplayName("Delete dealer — removes dealer from repository")
    void delete_removesDealer() {
        UUID id = UUID.randomUUID();
        Dealer dealer = Dealer.builder().id(id).tenantId("tenant-1").build();

        when(dealerRepository.findByIdAndTenantId(id, "tenant-1")).thenReturn(Optional.of(dealer));

        dealerService.delete(id, "tenant-1");

        verify(dealerRepository).delete(dealer);
    }

    @Test
    @DisplayName("Delete dealer — throws exception if not found")
    void delete_throwsIfNotFound() {
        UUID id = UUID.randomUUID();
        when(dealerRepository.findByIdAndTenantId(id, "tenant-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> dealerService.delete(id, "tenant-1"));
    }
}
