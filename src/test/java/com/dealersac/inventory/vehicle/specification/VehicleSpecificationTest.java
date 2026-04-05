package com.dealersac.inventory.vehicle.specification;

import com.dealersac.inventory.dealer.domain.Dealer;
import com.dealersac.inventory.dealer.domain.SubscriptionType;
import com.dealersac.inventory.vehicle.domain.Vehicle;
import com.dealersac.inventory.vehicle.domain.VehicleStatus;
import com.dealersac.inventory.vehicle.dto.VehicleFilter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Unit Tests — Vehicle Specification")
class VehicleSpecificationTest {

    @Test
    @DisplayName("Build specification from filter — check tenant equivalence predicate")
    void buildFrom_withTenantId_addsTenantPredicate() {
        VehicleFilter filter = VehicleFilter.builder()
                .tenantId("alpha-tenant")
                .build();

        Specification<Vehicle> spec = VehicleSpecification.buildFrom(filter);

        Root<Vehicle> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        when(root.get(anyString())).thenReturn(mock(jakarta.persistence.criteria.Path.class));
        when(cb.equal(any(), any())).thenReturn(mock(Predicate.class));

        spec.toPredicate(root, query, cb);

        verify(cb).equal(any(), eq("alpha-tenant"));
    }

    @Test
    @DisplayName("Build specification with all filters — adds all predicates")
    void buildFrom_withAllFilters_addsAllPredicates() {
        VehicleFilter filter = VehicleFilter.builder()
                .tenantId("alpha-tenant")
                .model("Tesla")
                .status(VehicleStatus.AVAILABLE)
                .priceMin(java.math.BigDecimal.valueOf(20000))
                .priceMax(java.math.BigDecimal.valueOf(80000))
                .build();

        Specification<Vehicle> spec = VehicleSpecification.buildFrom(filter);

        Root<Vehicle> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        
        when(root.get(anyString())).thenReturn(mock(jakarta.persistence.criteria.Path.class));
        when(cb.equal(any(), any())).thenReturn(mock(Predicate.class));
        when(cb.like(any(), anyString())).thenReturn(mock(Predicate.class));
        when(cb.greaterThanOrEqualTo(any(), any(java.math.BigDecimal.class))).thenReturn(mock(Predicate.class));
        when(cb.lessThanOrEqualTo(any(), any(java.math.BigDecimal.class))).thenReturn(mock(Predicate.class));
        when(cb.lower(any())).thenReturn(mock(jakarta.persistence.criteria.Expression.class));

        spec.toPredicate(root, query, cb);

        verify(cb).equal(any(), eq("alpha-tenant"));
        verify(cb).equal(any(), eq(VehicleStatus.AVAILABLE));
    }

    @Test
    @DisplayName("Build specification with PREMIUM subscription — adds subquery")
    void buildFrom_withPremiumSubscription_addsSubquery() {
        VehicleFilter filter = VehicleFilter.builder()
                .tenantId("alpha-tenant")
                .subscription(SubscriptionType.PREMIUM)
                .build();

        Specification<Vehicle> spec = VehicleSpecification.buildFrom(filter);

        Root<Vehicle> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        jakarta.persistence.criteria.Subquery<UUID> subquery = mock(jakarta.persistence.criteria.Subquery.class);
        Root<Dealer> dealerRoot = mock(Root.class);

        when(root.get(anyString())).thenReturn(mock(jakarta.persistence.criteria.Path.class));
        when(dealerRoot.get(anyString())).thenReturn(mock(jakarta.persistence.criteria.Path.class));
        when(query.subquery(UUID.class)).thenReturn(subquery);
        when(subquery.from(Dealer.class)).thenReturn(dealerRoot);
        when(subquery.select(any())).thenReturn(subquery);
        when(subquery.where(any(Predicate[].class))).thenReturn(subquery);
        when(cb.equal(any(), any())).thenReturn(mock(Predicate.class));

        spec.toPredicate(root, query, cb);

        verify(query).subquery(UUID.class);
        verify(subquery).from(Dealer.class);
    }
}
