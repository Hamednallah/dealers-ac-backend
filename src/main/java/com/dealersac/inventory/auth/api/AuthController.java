package com.dealersac.inventory.auth.api;

import com.dealersac.inventory.auth.application.AuthService;
import com.dealersac.inventory.auth.dto.LoginRequest;
import com.dealersac.inventory.auth.dto.RegisterRequest;
import com.dealersac.inventory.auth.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register and login endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new tenant user",
               description = "Creates a new TENANT_USER and returns a JWT token.")
    public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Login and obtain a JWT",
               description = "Rate limited: 5 requests per minute per IP.")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
