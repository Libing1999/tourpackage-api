package com.tourpackage.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    // Paths here are matched against the servlet path with the
    // server.servlet.context-path ("/api") already stripped — so these are
    // "/auth/login" etc., NOT "/api/auth/login". Only the truly anonymous
    // auth endpoints; GET/PUT /auth/profile fall through to
    // anyRequest().authenticated() below.
    private static final String[] PUBLIC_ENDPOINTS = {
            "/auth/login",
            "/auth/refresh",
            "/auth/forgot-password",
            "/auth/reset-password",
            "/auth/verify-email",
            // Read-only marketing-site content (homepage sections) plus the
            // one write endpoint a visitor can hit anonymously.
            "/public/**",
            // Uploaded images are public assets — they're rendered on the
            // marketing site by unauthenticated visitors. Writing them still
            // requires an admin (see AdminMediaController).
            "/uploads/**",
            "/actuator/health",
            "/actuator/health/**",
            // Both forms: /swagger-ui.html is the entry point that redirects
            // into /swagger-ui/**, and permitting only the latter leaves the
            // documented URL returning 401.
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                // Response headers a browser acts on. This is a JSON API, so
                // most of the classic set is irrelevant — but these three are not:
                .headers(headers -> headers
                        // Stops a browser second-guessing Content-Type. An
                        // uploaded file served as an image must never be
                        // sniffed into being executed as something else.
                        .contentTypeOptions(withDefaults -> {})
                        // HSTS. Only meaningful over HTTPS, and ignored by
                        // browsers on plain HTTP, so it is safe to send always.
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        // Referrer leaks matter here: password-reset and
                        // verification links carry tokens in the query string,
                        // and the default policy would send them to any host the
                        // page links out to.
                        .referrerPolicy(referrer -> referrer.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
                                        .ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .frameOptions(frame -> frame.deny())
                        // A JSON API renders nothing, so the strictest possible
                        // policy is also the correct one — it only takes effect
                        // if a response is ever rendered as a document.
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'none'; frame-ancestors 'none'; sandbox")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Backs the login step only ({@code AuthService} calls this directly).
     * Delegates to the {@link AdminUserDetailsService} + {@link PasswordEncoder}
     * that Spring Boot auto-wires into a DaoAuthenticationProvider for us.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

}
