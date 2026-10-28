package gr.hua.dit.dras.services.domain;

/* imports */
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImageStorageServiceTest {

    @TempDir
    Path tempUploadDir;

    private ImageStorageService imageStorageService;

    @BeforeEach
    void setUp() {
        imageStorageService = new ImageStorageService(tempUploadDir.toString());
    }

    @Test
    void testStoreImages_emptyOrNull() {
        assertThat(imageStorageService.storeImages(null)).isEmpty();
        assertThat(imageStorageService.storeImages(Collections.emptyList())).isEmpty();
    }

    @Test
    void testStoreImages_invalidExtensionAndContentType() {
        MockMultipartFile exeFile = new MockMultipartFile(
                "images", "script.sh", "text/x-shellscript", "#!/bin/bash".getBytes()
        );
        MockMultipartFile badMime = new MockMultipartFile(
                "images", "photo.jpg", "application/pdf", "dummy pdf content".getBytes()
        );

        List<String> paths = imageStorageService.storeImages(List.of(exeFile, badMime));
        assertThat(paths).isEmpty();
    }

    @Test
    void testStoreImages_resizesAndCompressesLargeJpeg() throws IOException {
        // Create a large 2000x1000 test image in memory
        BufferedImage originalImage = new BufferedImage(2000, 1000, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = originalImage.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 2000, 1000);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(originalImage, "jpg", baos);
        byte[] imageBytes = baos.toByteArray();

        MockMultipartFile multipartFile = new MockMultipartFile(
                "images", "large-living-room.jpg", "image/jpeg", imageBytes
        );

        List<String> storedPaths = imageStorageService.storeImages(List.of(multipartFile));

        assertThat(storedPaths).hasSize(1);
        String storedPath = storedPaths.get(0);
        assertThat(storedPath).startsWith("/images/").endsWith(".jpg");

        // Verify the saved file on disk
        String filename = storedPath.replace("/images/", "");
        Path savedFile = tempUploadDir.resolve(filename);
        assertThat(Files.exists(savedFile)).isTrue();

        // Check resized dimensions
        BufferedImage savedImage = ImageIO.read(savedFile.toFile());
        assertThat(savedImage).isNotNull();
        assertThat(savedImage.getWidth()).isEqualTo(1400); // 2000 scaled down to 1400
        assertThat(savedImage.getHeight()).isEqualTo(700); // 1000 scaled down by same factor
    }

    @Test
    void testStoreImages_preservesPngFormat() throws IOException {
        BufferedImage originalPng = new BufferedImage(500, 400, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(originalPng, "png", baos);

        MockMultipartFile multipartFile = new MockMultipartFile(
                "images", "floorplan.png", "image/png", baos.toByteArray()
        );

        List<String> storedPaths = imageStorageService.storeImages(List.of(multipartFile));
        assertThat(storedPaths).hasSize(1);
        assertThat(storedPaths.get(0)).startsWith("/images/").endsWith(".png");

        String filename = storedPaths.get(0).replace("/images/", "");
        Path savedFile = tempUploadDir.resolve(filename);
        assertThat(Files.exists(savedFile)).isTrue();

        BufferedImage savedImage = ImageIO.read(savedFile.toFile());
        assertThat(savedImage).isNotNull();
        assertThat(savedImage.getWidth()).isEqualTo(500);
        assertThat(savedImage.getHeight()).isEqualTo(400);
    }
}
