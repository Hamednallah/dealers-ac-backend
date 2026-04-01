package com.dealersac.inventory.vehicle.dto;

import com.dealersac.inventory.dealer.domain.SubscriptionType;
import com.dealersac.inventory.vehicle.domain.VehicleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Schema(description = "Create vehicle request")
public class VehicleRequest {

    @NotNull(message = "Dealer ID is required")
    @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID dealerId;

    @NotBlank(message = "Model is required")
    @Size(max = 255)
    @Schema(example = "Toyota Camry 2024")
    private String model;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 10, fraction = 2)
    @Schema(example = "29999.99")
    private BigDecimal price;

    @Schema(example = "AVAILABLE", defaultValue = "AVAILABLE")
    private VehicleStatus status = VehicleStatus.AVAILABLE;
}
