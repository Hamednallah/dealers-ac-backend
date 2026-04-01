package com.dealersac.inventory.auth.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "Registration request")
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    @Schema(example = "john.doe")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(example = "SecurePass@123", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;

    @Email(message = "Must be a valid email address")
    @Schema(example = "john@example.com")
    private String email;

    @NotBlank(message = "Tenant ID is required")
    @Schema(example = "tenant-alpha")
    private String tenantId;
}
