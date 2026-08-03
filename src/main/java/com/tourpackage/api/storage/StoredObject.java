package com.tourpackage.api.storage;

/**
 * What a provider returns after writing bytes: the key it can later be
 * addressed by, and the URL a browser can fetch it from. These differ per
 * provider — a local path vs. an S3 object key vs. a Cloudinary public_id —
 * which is exactly why callers store both rather than deriving one from the other.
 */
public record StoredObject(String storageKey, String url) {
}
