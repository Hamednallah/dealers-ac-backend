package com.dealersac.inventory.vehicle.dto;

import com.dealersac.inventory.vehicle.domain.Vehicle;
import com.dealersac.inventory.vehicle.domain.VehicleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@Schema(description = "Vehicle response")
public class VehicleResponse {

    private UUID          id;
    private String        tenantId;
    private UUID          dealerId;
    private String        model;
    private BigDecimal    price;
    private VehicleStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static VehicleResponse from(Vehicle v) {
        return VehicleResponse.builder()
                .id(v.getId())
                .tenantId(v.getTenantId())
                .dealerId(v.getDealerId())
                .model(v.getModel())
                .price(v.getPrice())
                .status(v.getStatus())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }
}
