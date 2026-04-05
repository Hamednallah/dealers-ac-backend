package com.dealersac.inventory.common.audit;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    void deleteByCreatedAtBefore(LocalDateTime before);
}
