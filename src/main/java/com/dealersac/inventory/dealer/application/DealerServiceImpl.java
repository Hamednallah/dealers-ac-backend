package com.dealersac.inventory.dealer.application;

import com.dealersac.inventory.common.audit.Audited;
import com.dealersac.inventory.common.exception.CrossTenantAccessException;
import com.dealersac.inventory.common.exception.DuplicateResourceException;
import com.dealersac.inventory.common.exception.ResourceNotFoundException;
import com.dealersac.inventory.common.webhook.DomainEvent;
import com.dealersac.inventory.dealer.domain.Dealer;
import com.dealersac.inventory.dealer.dto.DealerPatchRequest;
import com.dealersac.inventory.dealer.dto.DealerRequest;
import com.dealersac.inventory.dealer.dto.DealerResponse;
import com.dealersac.inventory.dealer.repository.DealerRepository;
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
public class DealerServiceImpl implements DealerService {

    private final DealerRepository       dealerRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    @Audited(action = "CREATE_DEALER", entityType = "Dealer")
    @PreAuthorize("hasRole('TENANT_USER') or hasRole('GLOBAL_ADMIN')")
    public DealerResponse create(DealerRequest request, String tenantId) {
        if (dealerRepository.existsByTenantIdAndEmail(tenantId, request.getEmail())) {
            throw new DuplicateResourceException(
                    "A dealer with email '" + request.getEmail() + "' already exists in this tenant");
        }

        Dealer dealer = Dealer.builder()
                .tenantId(tenantId)
                .name(request.getName())
                .email(request.getEmail())
                .subscriptionType(request.getSubscriptionType())
                .build();

        dealer = dealerRepository.save(dealer);
        log.info("Dealer created: {} in tenant: {}", dealer.getId(), tenantId);

        eventPublisher.publishEvent(new DomainEvent(this, "DEALER_CREATED", tenantId,
                Map.of("dealerId", dealer.getId().toString(), "name", dealer.getName())));

        return DealerResponse.from(dealer);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('TENANT_USER') or hasRole('GLOBAL_ADMIN')")
    public DealerResponse findById(UUID id, String tenantId) {
        Dealer dealer = dealerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer", id));

        // Security: 404 for cross-tenant to avoid exposing resource existence
        if (!dealer.getTenantId().equals(tenantId)) {
            throw new CrossTenantAccessException();
        }

        return DealerResponse.from(dealer);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('TENANT_USER') or hasRole('GLOBAL_ADMIN')")
    public Page<DealerResponse> findAll(String tenantId, Pageable pageable) {
        return dealerRepository.findAllByTenantId(tenantId, pageable)
                .map(DealerResponse::from);
    }

    @Override
    @Transactional
    @Audited(action = "UPDATE_DEALER", entityType = "Dealer")
    @PreAuthorize("hasRole('TENANT_USER') or hasRole('GLOBAL_ADMIN')")
    public DealerResponse update(UUID id, DealerPatchRequest patch, String tenantId) {
        Dealer dealer = dealerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer", id));

        if (patch.getName() != null)             dealer.setName(patch.getName());
        if (patch.getEmail() != null)            dealer.setEmail(patch.getEmail());
        if (patch.getSubscriptionType() != null) dealer.setSubscriptionType(patch.getSubscriptionType());

        return DealerResponse.from(dealerRepository.save(dealer));
    }

    @Override
    @Transactional
    @Audited(action = "DELETE_DEALER", entityType = "Dealer")
    @PreAuthorize("hasRole('TENANT_USER') or hasRole('GLOBAL_ADMIN')")
    public void delete(UUID id, String tenantId) {
        Dealer dealer = dealerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer", id));

        dealerRepository.delete(dealer);
        log.info("Dealer deleted: {} from tenant: {}", id, tenantId);
    }
}
