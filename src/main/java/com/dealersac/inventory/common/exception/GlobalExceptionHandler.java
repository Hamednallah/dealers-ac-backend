package com.dealersac.inventory.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralized exception → HTTP response mapping (RFC 7807 ProblemDetail).
 *
 * Security: NO stack traces, NO internal details in production responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ── 400 Bad Request ───────────────────────────────────────────────────────

    @ExceptionHandler(MissingTenantException.class)
    public ProblemDetail handleMissingTenant(MissingTenantException ex) {
        log.warn("Missing tenant: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setType(URI.create("/errors/missing-tenant"));
        return pd;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setType(URI.create("/errors/bad-request"));
        return pd;
    }

    // ── 401 Unauthorized ──────────────────────────────────────────────────────

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication required");
        pd.setType(URI.create("/errors/unauthorized"));
        return pd;
    }

    // ── 403 Forbidden ─────────────────────────────────────────────────────────

    @ExceptionHandler(CrossTenantAccessException.class)
    public ProblemDetail handleCrossTenant(CrossTenantAccessException ex) {
        log.warn("Cross-tenant access attempt blocked");
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access to this resource is forbidden");
        pd.setType(URI.create("/errors/forbidden"));
        return pd;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
        pd.setType(URI.create("/errors/forbidden"));
        return pd;
    }

    // ── 404 Not Found ─────────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create("/errors/not-found"));
        return pd;
    }

    // ── 409 Conflict ──────────────────────────────────────────────────────────

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create("/errors/conflict"));
        return pd;
    }

    // ── 422 Validation ────────────────────────────────────────────────────────

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (a, b) -> a));

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, "Validation failed");
        pd.setType(URI.create("/errors/validation"));
        pd.setProperty("errors", fieldErrors);
        return ResponseEntity.unprocessableEntity().body(pd);
    }

    // ── 429 Rate Limit ────────────────────────────────────────────────────────

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimit(RateLimitExceededException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please slow down.");
        pd.setType(URI.create("/errors/rate-limit"));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", "60")
                .body(pd);
    }

    // ── 500 Fallback ──────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        // Never expose internal details
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        pd.setType(URI.create("/errors/internal"));
        return pd;
    }
}
