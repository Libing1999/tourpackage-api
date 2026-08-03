package com.tourpackage.api.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves locally-stored uploads over HTTP.
 *
 * <p>Only registered when the local provider is active — with S3 or Cloudinary
 * the URL points at the provider and nothing should be served from this app.
 */
@Configuration
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class WebConfig implements WebMvcConfigurer {

    private final Path root;
    private final String servePath;

    public WebConfig(
            @Value("${app.storage.local.directory}") String directory,
            @Value("${app.storage.local.serve-path}") String servePath) {
        this.root = Paths.get(directory).toAbsolutePath().normalize();
        this.servePath = servePath;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(servePath + "/**")
                .addResourceLocations(root.toUri().toString())
                // Filenames contain a UUID and are never reused, so the content
                // at a given URL can't change — it's safe to cache hard.
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic());
    }

}
