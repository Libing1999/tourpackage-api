package com.tourpackage.api.observability;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Reports whether uploads can actually be written.
 *
 * <p>Worth its own check because the failure is silent otherwise: a container
 * restarted without its volume, or a full disk, leaves the application healthy
 * by every other measure while every image upload fails. The database being up
 * says nothing about this.
 *
 * <p>Only registered for local storage — with S3 or Cloudinary the equivalent
 * check is a network call on a schedule, which is a different design.
 */
@Component("storage")
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class StorageHealthIndicator implements HealthIndicator {

    private final Path root;

    public StorageHealthIndicator(@Value("${app.storage.local.directory}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override
    public Health health() {
        try {
            if (!Files.isDirectory(root)) {
                return Health.down().withDetail("path", root.toString())
                        .withDetail("reason", "upload directory does not exist").build();
            }
            if (!Files.isWritable(root)) {
                return Health.down().withDetail("path", root.toString())
                        .withDetail("reason", "upload directory is not writable").build();
            }

            long usableMb = Files.getFileStore(root).getUsableSpace() / (1024 * 1024);
            Health.Builder builder = usableMb < 100 ? Health.down() : Health.up();
            return builder
                    .withDetail("path", root.toString())
                    .withDetail("usableSpaceMb", usableMb)
                    .build();
        } catch (IOException ex) {
            return Health.down(ex).withDetail("path", root.toString()).build();
        }
    }

}
