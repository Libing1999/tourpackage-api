package com.tourpackage.api.security;

import java.util.UUID;

import com.tourpackage.api.entity.AdminRole;

/**
 * Lightweight principal placed in the {@code SecurityContext} by
 * {@link JwtAuthenticationFilter} for every authenticated request. Carries
 * only what's inside the access token's claims — controllers that need the
 * full {@code Admin} row look it up by {@link #id()}.
 */
public record AuthenticatedAdmin(UUID id, String email, AdminRole role) {
}
