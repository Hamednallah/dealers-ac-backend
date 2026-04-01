package com.dealersac.inventory.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Login request")
public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Schema(example = "john.doe")
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(example = "SecurePass@123", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;
}
