package com.dealersac.inventory.common.exception;

public class CrossTenantAccessException extends RuntimeException {
    public CrossTenantAccessException() {
        super("Access to resource belonging to a different tenant is forbidden");
    }
}
