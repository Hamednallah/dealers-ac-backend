package com.dealersac.inventory.dealer.dto;

import com.dealersac.inventory.dealer.domain.SubscriptionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
@Schema(description = "Partial update dealer request — all fields optional")
public class DealerPatchRequest {

    @Schema(example = "Beta Motors")
    private String name;

    @Email(message = "Must be a valid email")
    @Schema(example = "new@betamotors.com")
    private String email;

    @Schema(example = "BASIC")
    private SubscriptionType subscriptionType;
}
