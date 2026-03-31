package com.dealersac.inventory.common.exception;

/**
 * Re-export IllegalArgumentException as a domain exception so
 * GlobalExceptionHandler can map it to 400 consistently.
 */
public class IllegalArgumentException extends RuntimeException {
    public IllegalArgumentException(String message) { super(message); }
}
