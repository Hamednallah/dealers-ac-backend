package com.dealersac.inventory.common.security;

import com.dealersac.inventory.common.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Validates JWT from Authorization header and sets SecurityContext.
 *
 * Security checks performed:
 * 1. Token signature valid
 * 2. Token not expired
 * 3. For TENANT_USER: JWT tenantId must match X-Tenant-Id header
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = jwtUtil.extractUsername(token);
        String role     = jwtUtil.extractRole(token);
        String jwtTenantId = jwtUtil.extractTenantId(token);

        // Tenant-user: JWT tenantId must match the X-Tenant-Id header
        if ("TENANT_USER".equals(role)) {
            String headerTenantId = TenantContext.getTenantId();
            if (headerTenantId != null && !headerTenantId.equals(jwtTenantId)) {
                log.warn("Tenant mismatch — JWT: {}, Header: {}, user: {}",
                        jwtTenantId, headerTenantId, username);
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Tenant ID mismatch between token and request header");
                return;
            }
        }

        // Set Spring Security context
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        var authentication = new UsernamePasswordAuthenticationToken(
                username, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
