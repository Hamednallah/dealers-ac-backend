package com.dealersac.inventory.common.exception;

public class MissingTenantException extends RuntimeException {
    public MissingTenantException(String message) { super(message); }
}
