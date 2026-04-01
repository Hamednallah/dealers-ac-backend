package com.dealersac.inventory.vehicle.dto;

import com.dealersac.inventory.dealer.domain.SubscriptionType;
import com.dealersac.inventory.vehicle.domain.VehicleStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Internal filter object assembled from query parameters in the controller.
 * Passed to VehicleSpecification to build JPA predicates.
 */
@Data
@Builder
public class VehicleFilter {
    private String           tenantId;
    private String           model;
    private VehicleStatus    status;
    private BigDecimal       priceMin;
    private BigDecimal       priceMax;
    private SubscriptionType subscription;
}
