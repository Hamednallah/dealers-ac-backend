package com.dealersac.inventory.admin.application;

import com.dealersac.inventory.dealer.domain.SubscriptionType;
import com.dealersac.inventory.dealer.repository.DealerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Admin service — counts across ALL tenants (global scope).
 *
 * Design Decision (documented in README):
 * The /admin/dealers/countBySubscription endpoint is intentionally GLOBAL —
 * it counts across all tenants combined, not per-tenant.
 * This is correct because only GLOBAL_ADMIN can access it,
 * and the purpose is operational insight, not tenant-level data access.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final DealerRepository dealerRepository;

    /**
     * Optimized counting using a grouped database query.
     *
     * Scalability Note:
     * This approach performs the aggregation at the database level using a GROUP BY query.
     * This is significantly more scalable than fetching all records into Java memory,
     * as it reduces both database transport overhead and application heap usage.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public Map<String, Long> countDealersBySubscription() {
        Map<String, Long> counts = new LinkedHashMap<>();
        // Pre-populate with all subscription types to ensure they appear in the result
        counts.put(SubscriptionType.BASIC.name(), 0L);
        counts.put(SubscriptionType.PREMIUM.name(), 0L);

        dealerRepository.countBySubscriptionType().forEach(result -> {
            SubscriptionType type = (SubscriptionType) result[0];
            Long count = (Long) result[1];
            counts.put(type.name(), count);
        });

        return counts;
    }
}
