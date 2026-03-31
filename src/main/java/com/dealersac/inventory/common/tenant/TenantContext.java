package com.dealersac.inventory.common.tenant;

/**
 * Holds the current request's tenant ID in a ThreadLocal.
 * Cleared after every request to prevent memory leaks.
 */
public final class TenantContext {

    private TenantContext() {}

    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();

    public static void setTenantId(String tenantId) {
        TENANT.set(tenantId);
    }

    public static String getTenantId() {
        return TENANT.get();
    }

    public static void clear() {
        TENANT.remove();
    }
}
