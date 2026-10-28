package gr.hua.dit.dras.services.domain;

/* imports */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

@Service
public class ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(ImageStorageService.class);

    public static final int MAX_IMAGE_DIMENSION = 1400;
    public static final float JPEG_COMPRESSION_QUALITY = 0.82f;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final Path uploadDir;

    public ImageStorageService(@Value("${dras.upload-dir:uploads/images}") String uploadDirStr) {
        Path p = Paths.get(uploadDirStr).toAbsolutePath().normalize();
        if (!Files.exists(p)) {
            Path parentCandidate = Paths.get("..", uploadDirStr).toAbsolutePath().normalize();
            if (Files.exists(parentCandidate)) {
                p = parentCandidate;
            }
        }
        try {
            Files.createDirectories(p);
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", p, e);
        }
        this.uploadDir = p;
    }

    public List<String> storeImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> storedPaths = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;

            String originalName = file.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
            }

            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                log.warn("Skipping file with disallowed extension: {}", originalName);
                continue;
            }

            String contentType = file.getContentType();
            if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
                log.warn("Skipping file with disallowed content type: {}", contentType);
                continue;
            }

            String filename = UUID.randomUUID().toString() + "." + extension;
            Path destination = uploadDir.resolve(filename).normalize();

            try {
                saveOptimizedImage(file, destination, extension);
                storedPaths.add("/images/" + filename);
            } catch (IOException e) {
                log.error("Failed to save uploaded file: {}", originalName, e);
            }
        }
        return storedPaths;
    }

    private void saveOptimizedImage(MultipartFile file, Path destination, String extension) throws IOException {
        String ext = extension.toLowerCase();
        // If not a standard raster format that ImageIO natively handles, copy directly
        if (!Set.of("jpg", "jpeg", "png").contains(ext)) {
            try (InputStream is = file.getInputStream()) {
                Files.copy(is, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return;
        }

        BufferedImage source;
        try (InputStream is = file.getInputStream()) {
            source = ImageIO.read(is);
        } catch (Exception e) {
            log.warn("ImageIO failed to parse {}, falling back to direct copy: {}", file.getOriginalFilename(), e.getMessage());
            try (InputStream is = file.getInputStream()) {
                Files.copy(is, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return;
        }

        if (source == null) {
            try (InputStream is = file.getInputStream()) {
                Files.copy(is, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return;
        }

        int origWidth = source.getWidth();
        int origHeight = source.getHeight();
        int targetWidth = origWidth;
        int targetHeight = origHeight;

        if (Math.max(origWidth, origHeight) > MAX_IMAGE_DIMENSION) {
            double scale = (double) MAX_IMAGE_DIMENSION / Math.max(origWidth, origHeight);
            targetWidth = Math.max(1, (int) Math.round(origWidth * scale));
            targetHeight = Math.max(1, (int) Math.round(origHeight * scale));
        }

        boolean isPng = "png".equals(ext);
        int imageType = isPng ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, imageType);

        Graphics2D g2d = resized.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (!isPng) {
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, targetWidth, targetHeight);
            }

            g2d.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g2d.dispose();
        }

        if (!isPng) {
            writeCompressedJpeg(resized, destination);
        } else {
            ImageIO.write(resized, "png", destination.toFile());
        }
    }

    private void writeCompressedJpeg(BufferedImage image, Path destination) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            ImageIO.write(image, "jpeg", destination.toFile());
            return;
        }

        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_COMPRESSION_QUALITY);
        }

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(destination.toFile())) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    public Path getUploadDir() {
        return uploadDir;
    }
}
