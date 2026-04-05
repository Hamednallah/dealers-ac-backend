package com.dealersac.inventory.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Tests — Global Exception Handler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("ResourceNotFoundException — returns 404")
    void handleResourceNotFound_returns404() {
        UUID id = UUID.randomUUID();
        ResourceNotFoundException ex = new ResourceNotFoundException("Dealer", id);

        ProblemDetail result = handler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatus());
        assertEquals("Dealer not found with id: " + id, result.getDetail());
    }

    @Test
    @DisplayName("IllegalArgumentException — returns 400 Bad Request")
    void handleIllegalArgument_returns400() {
        IllegalArgumentException ex = new IllegalArgumentException("Bad Request");

        ProblemDetail result = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatus());
        assertEquals("Bad Request", result.getDetail());
    }

    @Test
    @DisplayName("Security Exception — returns 403")
    void handleAccessDenied_returns403() {
        org.springframework.security.access.AccessDeniedException ex = 
                new org.springframework.security.access.AccessDeniedException("Denied");

        ProblemDetail result = handler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN.value(), result.getStatus());
    }
}
