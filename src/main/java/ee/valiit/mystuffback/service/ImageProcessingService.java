package ee.valiit.mystuffback.service;

import ee.valiit.mystuffback.infrastructure.exception.ForbiddenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;
import java.util.Set;

@Slf4j
@Service
public class ImageProcessingService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png");
    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024; // 10 MB
    private static final int MAX_DIMENSION = 1400;
    private static final float JPEG_QUALITY = 0.8f;

    public byte[] processBase64Image(String imageData) {
        if (imageData == null || imageData.isBlank()) {
            throw new ForbiddenException("Image data is missing");
        }

        String mimeType = extractMimeType(imageData);
        validateMimeType(mimeType);

        byte[] originalBytes = decodeBase64Payload(imageData);
        validateFileSize(originalBytes);

        BufferedImage inputImage = readImage(originalBytes);
        BufferedImage resizedImage = resizeIfNeeded(inputImage);

        return convertToJpegBytes(resizedImage);
    }

    private String extractMimeType(String imageData) {
        if (!imageData.startsWith("data:") || !imageData.contains(";base64,")) {
            throw new ForbiddenException("Invalid image format");
        }

        int mimeTypeStart = "data:".length();
        int mimeTypeEnd = imageData.indexOf(";base64,");
        return imageData.substring(mimeTypeStart, mimeTypeEnd);
    }

    private void validateMimeType(String mimeType) {
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new ForbiddenException("Only PNG and JPG/JPEG images are allowed");
        }
    }

    private byte[] decodeBase64Payload(String imageData) {
        try {
            String base64Payload = imageData.substring(imageData.indexOf(",") + 1);
            return Base64.getDecoder().decode(base64Payload);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to decode base64 image", e);
            throw new ForbiddenException("Invalid base64 image data");
        }
    }

    private void validateFileSize(byte[] imageBytes) {
        if (imageBytes.length > MAX_IMAGE_BYTES) {
            throw new ForbiddenException("Image is too large. Maximum allowed size is 10 MB");
        }
    }

    private BufferedImage readImage(byte[] imageBytes) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new ForbiddenException("Uploaded file is not a valid image");
            }
            return image;
        } catch (IOException e) {
            log.warn("Failed to read image bytes", e);
            throw new ForbiddenException("Could not read uploaded image");
        }
    }

    private BufferedImage resizeIfNeeded(BufferedImage originalImage) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        if (originalWidth <= MAX_DIMENSION && originalHeight <= MAX_DIMENSION) {
            return convertToRgbIfNeeded(originalImage);
        }

        double scale = Math.min(
                (double) MAX_DIMENSION / originalWidth,
                (double) MAX_DIMENSION / originalHeight
        );

        int newWidth = Math.max(1, (int) Math.round(originalWidth * scale));
        int newHeight = Math.max(1, (int) Math.round(originalHeight * scale));

        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resizedImage.createGraphics();

        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, newWidth, newHeight);
            graphics.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        } finally {
            graphics.dispose();
        }

        return resizedImage;
    }

    private BufferedImage convertToRgbIfNeeded(BufferedImage originalImage) {
        if (originalImage.getType() == BufferedImage.TYPE_INT_RGB) {
            return originalImage;
        }

        BufferedImage rgbImage = new BufferedImage(
                originalImage.getWidth(),
                originalImage.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D graphics = rgbImage.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, rgbImage.getWidth(), rgbImage.getHeight());
            graphics.drawImage(originalImage, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        return rgbImage;
    }

    private byte[] convertToJpegBytes(BufferedImage image) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) {
                throw new IllegalStateException("No JPEG writers available");
            }

            ImageWriter writer = writers.next();

            try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
                writer.setOutput(imageOutputStream);

                ImageWriteParam writeParam = writer.getDefaultWriteParam();
                if (writeParam.canWriteCompressed()) {
                    writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    writeParam.setCompressionQuality(JPEG_QUALITY);
                }

                writer.write(null, new IIOImage(image, null, null), writeParam);
            } finally {
                writer.dispose();
            }

            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Failed to convert image to JPEG", e);
            throw new ForbiddenException("Could not process uploaded image");
        }
    }
}