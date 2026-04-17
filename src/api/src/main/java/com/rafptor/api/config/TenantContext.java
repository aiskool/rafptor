package com.rafptor.api.config;

/**
 * ThreadLocal carrier for the authenticated tenant id. The
 * {@link com.rafptor.api.auth.JwtAuthenticationFilter} populates the value
 * on each request; services read it to scope MongoDB queries. Clearing is
 * done in a {@code try/finally} via {@link com.rafptor.api.config.TenantFilter}.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(String tenantId) {
        CURRENT.set(tenantId);
    }

    public static String get() {
        String value = CURRENT.get();
        if (value == null) {
            throw new IllegalStateException("no tenant id bound to current thread");
        }
        return value;
    }

    public static String getOrNull() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
