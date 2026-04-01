package com.dealersac.inventory.vehicle.dto;

import com.dealersac.inventory.vehicle.domain.VehicleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Partial update vehicle request — all fields optional")
public class VehiclePatchRequest {

    @Schema(example = "Honda Accord 2024")
    private String model;

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 10, fraction = 2)
    @Schema(example = "31500.00")
    private BigDecimal price;

    @Schema(example = "SOLD")
    private VehicleStatus status;
}
