package com.tourpackage.api.storage;

/**
 * Where uploaded files live. The only implementation today writes to local
 * disk; the interface exists so S3 or Cloudinary can be added without any
 * caller changing.
 *
 * <p><b>Adding a provider:</b> implement this interface, annotate it
 * {@code @Service} with {@code @ConditionalOnProperty(name = "app.storage.provider",
 * havingValue = "s3")}, and set {@code APP_STORAGE_PROVIDER=s3}. Nothing else
 * moves — {@code MediaService} depends on this interface, and
 * {@code media_assets.storage_provider} records which one wrote each row so a
 * later migration can find files still sitting on local disk.
 *
 * <p>The contract deliberately deals in {@code byte[]} rather than streams or
 * {@code MultipartFile}: images are processed in memory before they get here
 * (resized and re-encoded), and every candidate provider's SDK accepts bytes.
 * A provider needing streaming for large files would want a second method, not
 * a different shape for this one.
 */
public interface StorageService {

    /**
     * Writes {@code content} under a key derived from {@code folder} and
     * {@code filename}. Implementations must not overwrite an existing object:
     * callers pass an already-unique filename, and a collision means a bug
     * worth surfacing rather than silently replacing someone's file.
     */
    StoredObject store(String folder, String filename, byte[] content, String contentType);

    /** Removes the object. A key that no longer exists is not an error — the
     * end state is what the caller asked for either way. */
    void delete(String storageKey);

    StorageProvider provider();
}
