package com.dealersac.inventory.dealer.dto;

import com.dealersac.inventory.dealer.domain.SubscriptionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Create dealer request")
public class DealerRequest {

    @NotBlank(message = "Name is required")
    @Schema(example = "Alpha Motors")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email")
    @Schema(example = "contact@alphamotors.com")
    private String email;

    @NotNull(message = "Subscription type is required")
    @Schema(example = "PREMIUM")
    private SubscriptionType subscriptionType;
}
