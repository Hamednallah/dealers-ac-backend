package com.dealersac.inventory.auth.application;

import com.dealersac.inventory.auth.domain.Role;
import com.dealersac.inventory.auth.domain.User;
import com.dealersac.inventory.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a GLOBAL_ADMIN user on first startup if one doesn't exist.
 * Password is loaded from the ADMIN_PASSWORD environment variable.
 *
 * This is more secure than a hardcoded BCrypt hash in a Flyway migration
 * because the hash is generated at runtime using the actual PasswordEncoder strength.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.password}")
    private String adminPassword;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedAdminUser() {
        if (userRepository.existsByUsername("admin")) {
            log.info("Admin user already exists — skipping seed");
            return;
        }

        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode(adminPassword))
                .email("admin@dealersac.com")
                .tenantId(null)             // GLOBAL_ADMIN has no tenant
                .role(Role.GLOBAL_ADMIN)
                .build();

        userRepository.save(admin);
        log.info("GLOBAL_ADMIN user seeded successfully (username: admin)");
    }
}
