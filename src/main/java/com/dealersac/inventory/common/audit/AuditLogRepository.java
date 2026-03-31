package com.dealersac.inventory.common.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Modifying
    @Query("DELETE FROM AuditLog a WHERE a.createdAt < :before")
    void deleteOlderThan(LocalDateTime before);
}
