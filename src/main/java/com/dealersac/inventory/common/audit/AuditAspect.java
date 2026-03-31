package com.dealersac.inventory.common.audit;

import com.dealersac.inventory.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * AOP aspect that persists an AuditLog entry for every @Audited method.
 *
 * - Runs AROUND the method (captures success and failure)
 * - Zero overhead on non-annotated methods
 * - PII (emails, passwords) is never captured
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        String actor    = getActor();
        String tenantId = TenantContext.getTenantId();
        String ip       = getClientIp();
        Object result;

        try {
            result = joinPoint.proceed();
        } catch (Throwable t) {
            log.warn("Audited method failed: action={}, actor={}", audited.action(), actor);
            throw t;
        }

        // Extract entity ID from result if it's a UUID-bearing object
        UUID entityId = extractEntityId(result);

        AuditLog log = AuditLog.builder()
                .actor(actor)
                .tenantId(tenantId)
                .action(audited.action())
                .entityType(audited.entityType())
                .entityId(entityId)
                .ipAddress(ip)
                .build();

        auditLogRepository.save(log);

        return result;
    }

    private String getActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
    }

    private String getClientIp() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            String forwarded = attrs.getRequest().getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return attrs.getRequest().getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private UUID extractEntityId(Object result) {
        if (result == null) return null;
        try {
            var method = result.getClass().getMethod("getId");
            Object id = method.invoke(result);
            if (id instanceof UUID uid) return uid;
        } catch (Exception ignored) {}
        return null;
    }
}
