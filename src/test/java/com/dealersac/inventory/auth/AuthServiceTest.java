package com.dealersac.inventory.auth;

import com.dealersac.inventory.auth.application.AuthService;
import com.dealersac.inventory.auth.domain.Role;
import com.dealersac.inventory.auth.domain.User;
import com.dealersac.inventory.auth.dto.LoginRequest;
import com.dealersac.inventory.auth.dto.RegisterRequest;
import com.dealersac.inventory.auth.dto.TokenResponse;
import com.dealersac.inventory.auth.repository.UserRepository;
import com.dealersac.inventory.common.exception.DuplicateResourceException;
import com.dealersac.inventory.common.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock UserRepository  userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil         jwtUtil;

    @InjectMocks AuthService authService;

    @Test
    @DisplayName("register creates user and returns JWT")
    void register_success() {
        ReflectionTestUtils.setField(authService, "expirationMs", 900000L);

        RegisterRequest req = new RegisterRequest();
        req.setUsername("john");
        req.setPassword("password123");
        req.setEmail("john@example.com");
        req.setTenantId("tenant-a");

        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$hashed$");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return User.builder().id(UUID.randomUUID()).username(u.getUsername())
                    .password(u.getPassword()).tenantId(u.getTenantId())
                    .role(Role.TENANT_USER).build();
        });
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn("jwt-token");

        TokenResponse response = authService.register(req);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("register throws DuplicateResourceException for existing username")
    void register_duplicateUsername_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("existing");
        req.setPassword("pass");
        req.setTenantId("t");

        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("login returns JWT for valid credentials")
    void login_success() {
        ReflectionTestUtils.setField(authService, "expirationMs", 900000L);

        User user = User.builder().id(UUID.randomUUID()).username("john")
                .password("$hashed$").tenantId("t-a").role(Role.TENANT_USER).build();

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass123", "$hashed$")).thenReturn(true);
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn("jwt");

        LoginRequest req = new LoginRequest();
        req.setUsername("john");
        req.setPassword("pass123");

        TokenResponse response = authService.login(req);
        assertThat(response.getAccessToken()).isEqualTo("jwt");
    }

    @Test
    @DisplayName("login throws BadCredentialsException for wrong password")
    void login_wrongPassword_throws() {
        User user = User.builder().username("john").password("$hashed$")
                .role(Role.TENANT_USER).tenantId("t").build();

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "$hashed$")).thenReturn(false);

        LoginRequest req = new LoginRequest();
        req.setUsername("john");
        req.setPassword("wrong");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("login throws BadCredentialsException for non-existent user")
    void login_unknownUser_throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        LoginRequest req = new LoginRequest();
        req.setUsername("ghost");
        req.setPassword("pass");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }
}
