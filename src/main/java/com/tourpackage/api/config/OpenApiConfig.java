package com.tourpackage.api.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.models.GroupedOpenApi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * OpenAPI description and Swagger UI.
 *
 * <p>Split into two groups because the API has two audiences with nothing in
 * common. Everything under {@code /public/**} is callable by an anonymous
 * visitor; everything else needs a bearer token. One flat list of ~90 endpoints
 * makes it impossible to see at a glance which is which — and that distinction
 * is the single most important thing a reader needs from this document.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    OpenAPI apiDefinition(
            @Value("${app.api.version:1.0.0}") String version,
            @Value("${server.servlet.context-path:/api}") String contextPath,
            @Value("${app.api.public-url:http://localhost:8080}") String publicUrl) {
        return new OpenAPI()
                .info(new Info()
                        .title("TourPackage API")
                        .version(version)
                        .description("""
                                Travel agency booking platform.

                                **Authentication.** Admin endpoints take a bearer JWT from \
                                `POST /auth/login`. Access tokens are short-lived; use \
                                `POST /auth/refresh` rather than logging in again.

                                **Errors.** Every failure returns the same envelope: `status`, \
                                `error`, `message`, `path`, and `fieldErrors` for validation \
                                failures (a map of field name to message).

                                **Rate limits.** Responses carry `X-RateLimit-Limit` and \
                                `X-RateLimit-Remaining`. Exceeding a limit returns 429 with \
                                `Retry-After`.

                                **Tracing.** Every response carries `X-Request-Id`; quote it \
                                when reporting a problem.
                                """)
                        .contact(new Contact().name("TourPackage").email("support@tourpackage.com")))
                .servers(List.of(new Server().url(publicUrl + contextPath).description("API root")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token from POST /auth/login")))
                // Applied globally so the UI's Authorize button covers every
                // admin endpoint. The public group below opts out again.
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    @Bean
    GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .displayName("Public (no authentication)")
                .pathsToMatch("/public/**", "/auth/**")
                .build();
    }

    @Bean
    GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("Admin (bearer token required)")
                .pathsToMatch("/admin/**")
                .build();
    }

}
