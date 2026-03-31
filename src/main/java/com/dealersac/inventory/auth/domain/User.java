package com.dealersac.inventory.auth.domain;

import com.dealersac.inventory.common.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "password") // Never log passwords
public class User extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id")
    private String tenantId;    // NULL for GLOBAL_ADMIN

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;    // BCrypt hashed

    @Column
    private String email;       // PII — excluded from logs via @ToString(exclude)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
}
