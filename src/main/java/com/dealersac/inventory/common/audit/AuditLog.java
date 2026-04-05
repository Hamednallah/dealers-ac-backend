package com.dealersac.inventory.common.audit;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "audit_logs")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    private String id;

    private String actor;

    @Field("tenant_id")
    private String tenantId;

    private String action;

    @Field("entity_type")
    private String entityType;

    @Field("entity_id")
    private UUID entityId;

    @Field("ip_address")
    private String ipAddress;

    @Field("created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
