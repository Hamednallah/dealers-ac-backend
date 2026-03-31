package com.dealersac.inventory.common.ratelimit;

import com.dealersac.inventory.common.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting using Bucket4j (token bucket algorithm).
 *
 * Limits:
 * - POST /auth/login    → 5 req/min per IP  (brute-force protection)
 * - POST /auth/register → 10 req/min per IP
 * - All other endpoints → 100 req/min per IP
 */
@Slf4j
@Component
@Order(0)
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${app.rate-limit.auth-requests-per-minute:5}")
    private int authRpm;

    @Value("${app.rate-limit.register-requests-per-minute:10}")
    private int registerRpm;

    @Value("${app.rate-limit.api-requests-per-minute:100}")
    private int apiRpm;

    // Per-IP buckets — key: "IP::path-type"
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String ip       = getClientIp(request);
        String uri      = request.getRequestURI();
        String method   = request.getMethod();

        String bucketKey;
        int rpm;

        if ("POST".equals(method) && "/auth/login".equals(uri)) {
            bucketKey = ip + "::login";
            rpm       = authRpm;
        } else if ("POST".equals(method) && "/auth/register".equals(uri)) {
            bucketKey = ip + "::register";
            rpm       = registerRpm;
        } else {
            bucketKey = ip + "::api";
            rpm       = apiRpm;
        }

        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> buildBucket(rpm));

        if (!bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for IP: {} on path: {}", ip, uri);
            throw new RateLimitExceededException();
        }

        filterChain.doFilter(request, response);
    }

    private Bucket buildBucket(int requestsPerMinute) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(requestsPerMinute)
                .refillIntervally(requestsPerMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
