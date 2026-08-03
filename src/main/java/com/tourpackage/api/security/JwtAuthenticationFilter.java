package com.tourpackage.api.security;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.tourpackage.api.entity.AdminRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Reads the {@code Authorization: Bearer <token>} header, validates it via
 * {@link JwtService} and, when valid, populates the security context with
 * an {@link AuthenticatedAdmin} principal and a {@code ROLE_<role>}
 * authority taken directly from the token's claims — no database lookup on
 * the request path. Role changes take effect on the admin's next login
 * (i.e. next token issuance), not mid-session; that's an acceptable
 * trade-off for a stateless access token.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            if (SecurityContextHolder.getContext().getAuthentication() == null && !jwtService.isTokenExpired(token)) {
                String email = jwtService.extractSubject(token);
                UUID adminId = UUID.fromString(jwtService.extractClaim(token, claims -> claims.get("id", String.class)));
                AdminRole role = AdminRole.valueOf(jwtService.extractClaim(token, (Claims claims) -> claims.get("role", String.class)));

                AuthenticatedAdmin principal = new AuthenticatedAdmin(adminId, email, role);
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));

                var authToken = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

}
