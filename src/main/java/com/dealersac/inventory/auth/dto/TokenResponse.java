package com.dealersac.inventory.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "JWT token response")
public class TokenResponse {

    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(example = "Bearer")
    private final String tokenType = "Bearer";

    @Schema(description = "Expiry in milliseconds", example = "900000")
    private long expiresIn;
}
