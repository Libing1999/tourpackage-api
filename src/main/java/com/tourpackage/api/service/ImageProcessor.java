package com.tourpackage.api.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.tourpackage.api.exception.ApiException;

/**
 * Validates, resizes and re-encodes uploaded images.
 *
 * <p>Uses {@code javax.imageio} from the JDK rather than adding an imaging
 * library — the work here is decode, scale, encode, and the JDK does all three.
 * A library would earn its place if this needed EXIF orientation handling or
 * formats ImageIO doesn't ship with.
 */
@Component
public class ImageProcessor {

    /** What the app will decode. Deliberately a short allowlist: the check is
     * on actual decoded content, but refusing unexpected types up front means
     * never handing arbitrary bytes to a decoder. */
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final int maxDimension;
    private final int thumbnailDimension;
    private final float jpegQuality;
    private final long maxBytes;

    public ImageProcessor(
            @Value("${app.storage.image.max-dimension:2400}") int maxDimension,
            @Value("${app.storage.image.thumbnail-dimension:400}") int thumbnailDimension,
            @Value("${app.storage.image.jpeg-quality:0.82}") float jpegQuality,
            @Value("${app.storage.image.max-upload-bytes:10485760}") long maxBytes) {
        this.maxDimension = maxDimension;
        this.thumbnailDimension = thumbnailDimension;
        this.jpegQuality = jpegQuality;
        this.maxBytes = maxBytes;
    }

    /** A processed image, ready to store. */
    public record ProcessedImage(
            byte[] content, byte[] thumbnail, String contentType, String extension, int width, int height) {
    }

    public long maxUploadBytes() {
        return maxBytes;
    }

    public ProcessedImage process(byte[] original, String declaredContentType, String originalFilename) {
        if (original.length == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "'" + originalFilename + "' is empty");
        }
        if (original.length > maxBytes) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "'" + originalFilename + "' is larger than " + (maxBytes / 1_048_576) + " MB");
        }
        if (declaredContentType == null || !ALLOWED_CONTENT_TYPES.contains(declaredContentType.toLowerCase())) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "'" + originalFilename + "' is not a supported image (JPEG, PNG, WebP, or GIF)");
        }

        BufferedImage source = decode(original, originalFilename);

        // Transparency can't survive JPEG, so images that have it stay PNG and
        // everything else becomes JPEG — which is far smaller for photographs,
        // and photographs are what this app stores.
        boolean hasAlpha = usesTransparency(source);
        String outputFormat = hasAlpha ? "png" : "jpg";
        String outputContentType = hasAlpha ? "image/png" : "image/jpeg";

        BufferedImage resized = scaleToFit(source, maxDimension, hasAlpha);
        BufferedImage thumb = scaleToFit(source, thumbnailDimension, hasAlpha);

        return new ProcessedImage(
                encode(resized, outputFormat, originalFilename),
                encode(thumb, outputFormat, originalFilename),
                outputContentType,
                outputFormat,
                resized.getWidth(),
                resized.getHeight());
    }

    /**
     * Whether any pixel is actually less than fully opaque.
     *
     * <p>Deliberately not {@code getColorModel().hasAlpha()}, which only says the
     * buffer <em>has</em> an alpha channel. Anything drawn through an HTML canvas
     * — every image that comes back from the cropper — is RGBA even when every
     * pixel is opaque, so trusting the colour model stores cropped photographs
     * as PNG and throws away the JPEG compression they should have had.
     *
     * <p>Costs one pass over the pixels, and only for images that have a channel
     * to check; it stops at the first translucent pixel it finds.
     */
    private boolean usesTransparency(BufferedImage image) {
        if (!image.getColorModel().hasAlpha()) {
            return false;
        }
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0xFF) {
                    return true;
                }
            }
        }
        return false;
    }

    private BufferedImage decode(byte[] bytes, String filename) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                // ImageIO returns null rather than throwing when no reader
                // matches — which is what a file merely *claiming* to be an
                // image looks like.
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "'" + filename + "' could not be read as an image");
            }
            return image;
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "'" + filename + "' could not be read as an image");
        }
    }

    /** Scales down to fit within {@code max} on the longest side, preserving
     * aspect ratio. Images already smaller are returned untouched — upscaling
     * would add bytes without adding detail. */
    private BufferedImage scaleToFit(BufferedImage source, int max, boolean hasAlpha) {
        int width = source.getWidth();
        int height = source.getHeight();
        int longest = Math.max(width, height);

        if (longest <= max) {
            return ensureType(source, hasAlpha);
        }

        double scale = (double) max / longest;
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage target = new BufferedImage(
                targetWidth, targetHeight, hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);

        Graphics2D g = target.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        g.dispose();

        return target;
    }

    /** Redraws into a known colour model. A PNG with a palette or a JPEG read
     * as TYPE_CUSTOM will otherwise fail to encode, or encode with wrong
     * colours. */
    private BufferedImage ensureType(BufferedImage source, boolean hasAlpha) {
        int wanted = hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        if (source.getType() == wanted) {
            return source;
        }

        BufferedImage converted = new BufferedImage(source.getWidth(), source.getHeight(), wanted);
        Graphics2D g = converted.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return converted;
    }

    private byte[] encode(BufferedImage image, String format, String filename) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if ("png".equals(format)) {
                ImageIO.write(image, "png", out);
                return out.toByteArray();
            }

            // JPEG quality isn't exposed by ImageIO.write, so this goes through
            // the writer directly — that quality setting is the actual
            // compression the requirement asks for.
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (!writers.hasNext()) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No JPEG encoder available");
            }

            ImageWriter writer = writers.next();
            try (MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(out)) {
                writer.setOutput(ios);

                ImageWriteParam params = writer.getDefaultWriteParam();
                if (params.canWriteCompressed()) {
                    params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    params.setCompressionQuality(jpegQuality);
                }

                writer.write(null, new IIOImage(image, null, null), params);
            } finally {
                writer.dispose();
            }

            return out.toByteArray();
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not process '" + filename + "'");
        }
    }

}
