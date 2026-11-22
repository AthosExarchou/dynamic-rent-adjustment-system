package gr.hua.dit.dras.services.domain;

/* imports */
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.model.enums.PropertyType;
import gr.hua.dit.dras.model.enums.RentalDuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;

class ListingImagePolicyTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Local demo and owner upload paths are accepted in every environment")
    void acceptsLocalOwnerPaths() {
        ListingImagePolicy policy = new ListingImagePolicy(false, tempDir.toString());

        assertThat(policy.filterImages(List.of(
                "/images/demo/image1.jpg",
                "images/demo/image2.jpg",
                "/images/abc123.jpg"
        ))).containsExactly(
                "/images/demo/image1.jpg",
                "images/demo/image2.jpg",
                "/images/abc123.jpg"
        );
    }

    @Test
    @DisplayName("Remote URLs and encoded traversal paths are rejected")
    void rejectsRemoteAndTraversalPaths() {
        ListingImagePolicy policy = new ListingImagePolicy(true, tempDir.toString());

        assertThat(policy.filterImages(List.of(
                "https://cdn.example.com/photo.jpg",
                "http://cdn.example.com/photo.jpg",
                "//cdn.example.com/photo.jpg",
                "data/images/../secret.jpg",
                "data/images/%2e%2e/secret.jpg",
                "data/images/%252e%252e%252fsecret.jpg",
                "images/%252e%252e%252fsecret.jpg",
                "data/images/one\\two.jpg",
                "data:image/png;base64,abc"
        ))).isEmpty();
    }

    @Test
    @DisplayName("Scraper paths require an existing local file and an enabled pipeline")
    void handlesScraperPaths() throws Exception {
        Path image = tempDir.resolve("listing-1").resolve("a.jpg");
        Files.createDirectories(image.getParent());
        Files.writeString(image, "test");

        ListingImagePolicy devPolicy = new ListingImagePolicy(true, tempDir.toString());
        assertThat(devPolicy.forExternalImport(List.of(
                "data/images/listing-1/a.jpg",
                "data/images/listing-1/missing.jpg",
                "https://spitogatos.gr/a.jpg"
        ))).containsExactly("data/images/listing-1/a.jpg");

        ListingImagePolicy prodPolicy = new ListingImagePolicy(false, tempDir.toString());
        assertThat(prodPolicy.forExternalImport(List.of("data/images/listing-1/a.jpg"))).isEmpty();

        Listing external = new Listing();
        external.setExternal(true);
        external.setImages(List.of("data/images/listing-1/a.jpg"));
        assertThat(prodPolicy.forListing(external)).isEmpty();
        assertThat(devPolicy.forListing(external)).containsExactly("data/images/listing-1/a.jpg");
    }

    @Test
    @DisplayName("Local non-external listings keep remote URLs out of responses")
    void filtersRemoteUrlsOnRead() {
        ListingImagePolicy policy = new ListingImagePolicy(false, tempDir.toString());
        Listing listing = new Listing();
        listing.setExternal(false);
        listing.setPropertyType(PropertyType.APARTMENT);
        listing.setRentalDuration(RentalDuration.LONG_TERM);
        listing.setImages(List.of("https://cdn.example.com/photo.jpg", "/images/demo/image1.jpg"));

        assertThat(policy.forListing(listing)).containsExactly("/images/demo/image1.jpg");
    }

    @Test
    @DisplayName("Relative image directory falls back to the parent working directory")
    void resolvesRelativeImageDirFromParent() throws Exception {
        Path currentDir = Path.of("").toAbsolutePath().normalize();
        Path parentDir = currentDir.getParent();
        String relativeDir = "dras-image-dir-" + UUID.randomUUID();
        Path parentCandidate = parentDir.resolve(relativeDir);
        Path image = parentCandidate.resolve("listing-1").resolve("a.jpg");
        Files.createDirectories(image.getParent());
        Files.writeString(image, "test");

        try {
            assertThat(Files.exists(currentDir.resolve(relativeDir))).isFalse();
            ListingImagePolicy policy = new ListingImagePolicy(true, relativeDir);

            assertThat(policy.forExternalImport(List.of("data/images/listing-1/a.jpg")))
                    .containsExactly("data/images/listing-1/a.jpg");
        } finally {
            deleteRecursively(parentCandidate);
        }
    }

    private void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
