package com.dealersac.inventory.auth.application;

import com.dealersac.inventory.auth.domain.Role;
import com.dealersac.inventory.auth.domain.User;
import com.dealersac.inventory.auth.dto.LoginRequest;
import com.dealersac.inventory.auth.dto.RegisterRequest;
import com.dealersac.inventory.auth.dto.TokenResponse;
import com.dealersac.inventory.auth.repository.UserRepository;
import com.dealersac.inventory.common.exception.DuplicateResourceException;
import com.dealersac.inventory.common.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil         jwtUtil;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .tenantId(request.getTenantId())
                .role(Role.TENANT_USER)
                .build();

        userRepository.save(user);
        log.info("User registered: {} in tenant: {}", user.getUsername(), user.getTenantId());

        String token = jwtUtil.generateToken(user.getUsername(), user.getTenantId(), user.getRole().name());
        return new TokenResponse(token, expirationMs);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        log.info("User logged in: {}", user.getUsername());
        String token = jwtUtil.generateToken(
                user.getUsername(),
                user.getTenantId(),
                user.getRole().name());
        return new TokenResponse(token, expirationMs);
    }
}
