package com.rafptor.api.config;

/**
 * Extra context attached to an Authentication via {@code setDetails}.
 * Carries the tenant id parsed out of the JWT.
 */
public record AuthDetails(String tenantId, String userId) {
}
