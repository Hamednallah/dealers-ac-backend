package com.dealersac.inventory.common.audit;

import com.dealersac.inventory.common.tenant.TenantContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests — Audit Aspect")
class AuditAspectTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Audited audited;

    @InjectMocks
    private AuditAspect auditAspect;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("test-tenant");
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Audit successful execution — captures log and entity ID")
    void audit_success() throws Throwable {
        UUID entityId = UUID.randomUUID();
        Object result = new Object() {
            public UUID getId() { return entityId; }
        };

        when(audited.action()).thenReturn("CREATE_VEHICLE");
        when(audited.entityType()).thenReturn("VEHICLE");
        when(joinPoint.proceed()).thenReturn(result);

        // Mock security context
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("admin-user");
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        Object returned = auditAspect.audit(joinPoint, audited);

        assertEquals(result, returned);
        
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        
        AuditLog saved = captor.getValue();
        assertEquals("admin-user", saved.getActor());
        assertEquals("test-tenant", saved.getTenantId());
        assertEquals("CREATE_VEHICLE", saved.getAction());
        assertEquals(entityId, saved.getEntityId());
    }

    @Test
    @DisplayName("Audit failed execution — still throws exception")
    void audit_failure() throws Throwable {
        RuntimeException ex = new RuntimeException("DB Error");
        when(joinPoint.proceed()).thenThrow(ex);
        when(audited.action()).thenReturn("DELETE_DEALER");

        assertThrows(RuntimeException.class, () -> auditAspect.audit(joinPoint, audited));
        
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Audit anonymous execution")
    void audit_anonymous() throws Throwable {
        when(audited.action()).thenReturn("GET_REPORT");
        when(joinPoint.proceed()).thenReturn(null);

        auditAspect.audit(joinPoint, audited);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals("anonymous", captor.getValue().getActor());
    }
}
