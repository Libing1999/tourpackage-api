package com.tourpackage.api.service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tourpackage.api.dto.response.MediaAssetResponse;
import com.tourpackage.api.dto.response.PageResponse;
import com.tourpackage.api.entity.MediaAsset;
import com.tourpackage.api.exception.ApiException;
import com.tourpackage.api.exception.ResourceNotFoundException;
import com.tourpackage.api.repository.MediaAssetRepository;
import com.tourpackage.api.service.ImageProcessor.ProcessedImage;
import com.tourpackage.api.storage.StorageService;
import com.tourpackage.api.storage.StoredObject;

@Service
@Transactional
public class MediaService {

    /** Folders are part of a storage path, so they're restricted to something
     * that can't traverse or surprise a provider's key rules. */
    private static final Pattern SAFE_FOLDER = Pattern.compile("^[a-z0-9][a-z0-9-]{0,79}$");
    private static final int MAX_FILES_PER_UPLOAD = 20;

    private final MediaAssetRepository mediaAssetRepository;
    private final StorageService storageService;
    private final ImageProcessor imageProcessor;

    public MediaService(
            MediaAssetRepository mediaAssetRepository,
            StorageService storageService,
            ImageProcessor imageProcessor) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.storageService = storageService;
        this.imageProcessor = imageProcessor;
    }

    /**
     * Processes and stores several images at once.
     *
     * <p>All-or-nothing: one bad file fails the whole request. A partial
     * success would leave the client holding some URLs and an error, with no
     * way to tell which files landed — and the transaction rolls the rows back
     * regardless, so a partial result would mean orphaned files on disk.
     */
    public List<MediaAssetResponse> upload(List<MultipartFile> files, String folder, UUID uploadedBy) {
        if (files == null || files.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No files were uploaded");
        }
        if (files.size() > MAX_FILES_PER_UPLOAD) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "At most " + MAX_FILES_PER_UPLOAD + " images can be uploaded at once");
        }

        String safeFolder = normalizeFolder(folder);
        List<StoredObject> written = new ArrayList<>();

        try {
            List<MediaAsset> assets = new ArrayList<>();

            for (MultipartFile file : files) {
                ProcessedImage processed = imageProcessor.process(
                        readBytes(file), file.getContentType(), displayName(file));

                // Server-generated name: the client's filename is kept for
                // display but never used as a key, so a crafted name can't
                // decide where bytes land.
                String base = UUID.randomUUID().toString();
                StoredObject stored = storageService.store(
                        safeFolder, base + "." + processed.extension(),
                        processed.content(), processed.contentType());
                written.add(stored);

                StoredObject thumb = storageService.store(
                        safeFolder, base + "-thumb." + processed.extension(),
                        processed.thumbnail(), processed.contentType());
                written.add(thumb);

                assets.add(MediaAsset.builder()
                        .storageKey(stored.storageKey())
                        .url(stored.url())
                        .thumbnailUrl(thumb.url())
                        .originalFilename(displayName(file))
                        .contentType(processed.contentType())
                        .sizeBytes(processed.content().length)
                        .width(processed.width())
                        .height(processed.height())
                        .folder(safeFolder)
                        .storageProvider(storageService.provider())
                        .uploadedBy(uploadedBy)
                        .createdAt(Instant.now())
                        .build());
            }

            return mediaAssetRepository.saveAll(assets).stream().map(MediaService::toResponse).toList();
        } catch (RuntimeException ex) {
            // The DB transaction will roll back on its own; the filesystem
            // won't, so anything already written is removed by hand. Without
            // this, a rejected fifth file would leave four orphans on disk.
            written.forEach(object -> storageService.delete(object.storageKey()));
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<MediaAssetResponse> list(String folder, Pageable pageable) {
        var page = folder == null || folder.isBlank()
                ? mediaAssetRepository.findAllByOrderByCreatedAtDesc(pageable)
                : mediaAssetRepository.findByFolderOrderByCreatedAtDesc(normalizeFolder(folder), pageable);

        return PageResponse.of(page.map(MediaService::toResponse));
    }

    /**
     * Deletes the row and its files. The row goes first: a file left behind is
     * wasted disk, but a row pointing at a deleted file is a broken image on
     * the site.
     *
     * <p>Nothing here checks whether the URL is still referenced by a hotel,
     * package or gallery row — those columns hold plain URLs, including
     * external ones, so there's no foreign key to consult. Deleting a
     * still-referenced image leaves a broken link, which is why the admin UI
     * asks for confirmation.
     */
    public void delete(UUID id) {
        MediaAsset asset = mediaAssetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Media asset not found: " + id));

        mediaAssetRepository.delete(asset);

        storageService.delete(asset.getStorageKey());
        if (asset.getThumbnailUrl() != null) {
            storageService.delete(thumbnailKey(asset.getStorageKey()));
        }
    }

    /** The thumbnail sits beside its image with a {@code -thumb} suffix, so its
     * key is derived rather than stored in a second column. */
    private static String thumbnailKey(String storageKey) {
        int dot = storageKey.lastIndexOf('.');
        return dot < 0
                ? storageKey + "-thumb"
                : storageKey.substring(0, dot) + "-thumb" + storageKey.substring(dot);
    }

    private static String normalizeFolder(String folder) {
        String value = (folder == null || folder.isBlank() ? "general" : folder).trim().toLowerCase(Locale.ROOT);

        if (!SAFE_FOLDER.matcher(value).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Folder must be lowercase letters, numbers and hyphens");
        }
        return value;
    }

    private static String displayName(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            return "upload";
        }
        // Only the last segment: some browsers send a full path.
        String base = name.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        return base.length() > 255 ? base.substring(0, 255) : base;
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read uploaded file");
        }
    }

    private static MediaAssetResponse toResponse(MediaAsset a) {
        return new MediaAssetResponse(
                a.getId(), a.getUrl(), a.getThumbnailUrl(), a.getOriginalFilename(),
                a.getContentType(), a.getSizeBytes(), a.getWidth(), a.getHeight(),
                a.getFolder(), a.getCreatedAt());
    }

}
