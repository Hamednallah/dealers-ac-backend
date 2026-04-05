package com.dealersac.inventory.vehicle.specification;

import com.dealersac.inventory.dealer.domain.Dealer;
import com.dealersac.inventory.dealer.domain.SubscriptionType;
import com.dealersac.inventory.vehicle.domain.Vehicle;
import com.dealersac.inventory.vehicle.domain.VehicleStatus;
import com.dealersac.inventory.vehicle.dto.VehicleFilter;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Composable JPA Specifications for dynamic vehicle filtering.
 *
 * Avoids if-chain query methods — each predicate is independent and reusable.
 * The subscription=PREMIUM filter joins the dealers table and stays tenant-scoped.
 */
public class VehicleSpecification {

    private VehicleSpecification() {}

    public static Specification<Vehicle> buildFrom(VehicleFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // ── Always: tenant-scoped ──────────────────────────────────────
            predicates.add(cb.equal(root.get("tenantId"), filter.getTenantId()));

            // ── Optional filters ───────────────────────────────────────────
            if (filter.getModel() != null && !filter.getModel().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("model")),
                        "%" + filter.getModel().toLowerCase() + "%"));
            }

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getPriceMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filter.getPriceMin()));
            }

            if (filter.getPriceMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), filter.getPriceMax()));
            }

            // ── subscription=PREMIUM filter ────────────────────────────────
            // Joins dealers table and filters by subscriptionType.
            // Still tenant-scoped (vehicle.tenantId already added above).
            if (filter.getSubscription() == SubscriptionType.PREMIUM) {
                Subquery<UUID> subquery = query.subquery(UUID.class);
                Root<Dealer> dealerRoot = subquery.from(Dealer.class);
                subquery.select(dealerRoot.get("id"))
                        .where(
                            cb.equal(dealerRoot.get("subscriptionType"), SubscriptionType.PREMIUM),
                            cb.equal(dealerRoot.get("tenantId"), filter.getTenantId())
                        );
                predicates.add(root.get("dealerId").in(subquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
