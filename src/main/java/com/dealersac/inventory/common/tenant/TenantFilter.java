package com.dealersac.inventory.common.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

/**
 * Runs BEFORE Spring Security.
 * Extracts X-Tenant-Id from every request and stores it in TenantContext.
 * Clears the context in a finally block to prevent memory leaks.
 *
 * Paths in PUBLIC_PATHS bypass the tenant-header requirement entirely.
 * The /admin/ prefix is also bypassed because GLOBAL_ADMIN users are not
 * scoped to a single tenant.
 */
@Slf4j
@Component
@Order(1)
public class TenantFilter extends OncePerRequestFilter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Paths that don't require a tenant header */
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/auth/register",
            "/auth/login",
            "/admin/",
            "/actuator/health",
            "/actuator/info",
            "/swagger-ui",
            "/v3/api-docs",
            "/swagger-ui.html"
    );

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String tenantId = request.getHeader("X-Tenant-Id");
        if (tenantId == null || tenantId.isBlank()) {
            log.warn("Request to {} missing required X-Tenant-Id header", path);
            writeBadRequest(response, "Missing required header: X-Tenant-Id");
            return;
        }

        try {
            TenantContext.setTenantId(tenantId.trim());
            log.debug("Tenant context set: {}", tenantId);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            log.debug("Tenant context cleared");
        }
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private void writeBadRequest(HttpServletResponse response, String detail) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/problem+json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        String body = String.format(
                "{\"type\":\"/errors/missing-tenant\",\"title\":\"Bad Request\",\"status\":400,\"detail\":\"%s\"}",
                detail.replace("\"", "\\\""));
        response.getWriter().write(body);
    }
}
