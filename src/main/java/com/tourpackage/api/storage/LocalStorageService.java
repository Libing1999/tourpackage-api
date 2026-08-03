package com.tourpackage.api.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.tourpackage.api.exception.ApiException;

import jakarta.annotation.PostConstruct;

/**
 * Writes uploads to a directory on disk, served back by
 * {@code WebConfig}'s resource handler at {@code app.storage.public-base-url}.
 *
 * <p>The default provider, and the one that makes the app run with no external
 * accounts. It is genuinely local: files live on the machine that received
 * them, so more than one instance behind a load balancer would each hold a
 * different subset. That's the point at which S3 stops being optional.
 */
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path root;
    private final String publicBaseUrl;

    public LocalStorageService(
            @Value("${app.storage.local.directory}") String directory,
            @Value("${app.storage.public-base-url}") String publicBaseUrl) {
        this.root = Paths.get(directory).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    }

    @PostConstruct
    void ensureRootExists() {
        try {
            Files.createDirectories(root);
            log.info("Local media storage rooted at {}", root);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot create media storage directory: " + root, ex);
        }
    }

    @Override
    public StoredObject store(String folder, String filename, byte[] content, String contentType) {
        String key = folder + "/" + filename;
        Path target = resolveWithinRoot(key);

        try {
            Files.createDirectories(target.getParent());
            // CREATE_NEW rather than CREATE: the contract says never overwrite,
            // and letting the filesystem enforce it beats a check-then-write
            // race.
            Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store the uploaded file");
        }

        return new StoredObject(key, publicBaseUrl + "/" + key);
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolveWithinRoot(storageKey));
        } catch (IOException ex) {
            // The database row is already gone by this point; a file left
            // behind is wasted disk, not a broken reference, so it's logged
            // rather than failing the request.
            log.warn("Could not delete stored file {}: {}", storageKey, ex.getMessage());
        }
    }

    @Override
    public StorageProvider provider() {
        return StorageProvider.LOCAL;
    }

    /**
     * Resolves a key under the storage root and refuses anything that escapes
     * it. Keys are generated server-side today, but this is the one place a
     * caller-supplied {@code ../} would turn into writing anywhere on disk, so
     * it's checked here rather than trusted upstream.
     */
    private Path resolveWithinRoot(String storageKey) {
        Path resolved = root.resolve(storageKey).normalize();

        if (!resolved.startsWith(root)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid storage key");
        }
        return resolved;
    }

}
