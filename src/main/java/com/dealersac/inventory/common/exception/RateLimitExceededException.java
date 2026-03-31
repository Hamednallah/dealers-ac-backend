package com.dealersac.inventory.common.exception;

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException() { super("Rate limit exceeded"); }
}
