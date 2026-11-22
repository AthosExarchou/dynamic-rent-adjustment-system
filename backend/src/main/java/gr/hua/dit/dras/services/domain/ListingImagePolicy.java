package gr.hua.dit.dras.services.domain;

/* imports */
import gr.hua.dit.dras.entities.Listing;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Single allowlist for image paths accepted by the application.
 *
 * Remote URLs are never accepted. Local owner uploads and demo assets are
 * allowed in every environment. Downloaded scraper files are allowed only when
 * the external-image pipeline is enabled and the file exists locally.
 */
@Component
public class ListingImagePolicy {

    private static final String OWNER_IMAGE_PREFIX = "images/";
    private static final String SCRAPED_IMAGE_PREFIX = "data/images/";

    private final boolean externalImagesEnabled;
    private final Path externalImageRoot;

    public ListingImagePolicy(
            @Value("${dras.external-images.enabled:true}") boolean externalImagesEnabled,
            @Value("${dras.external-images.dir:analytics/data/images}") String externalImageDir
    ) {
        this.externalImagesEnabled = externalImagesEnabled;
        this.externalImageRoot = resolveImageDir(externalImageDir);
    }

    /**
     * Filters paths before storage and before they are sent to the UI.
     */
    public List<String> filterImages(Collection<String> images) {
        if (images == null) {
            return List.of();
        }

        return images.stream()
                .map(value -> value == null ? null : value.trim())
                .filter(this::isAllowed)
                .distinct()
                .toList();
    }

    /**
     * Applies the environment policy to a listing response.
     */
    public List<String> forListing(Listing listing) {
        if (listing == null) {
            return List.of();
        }
        if (!externalImagesEnabled && listing.isExternal()) {
            return List.of();
        }
        return filterImages(listing.getImages());
    }

    /**
     * Accepts only scraper-produced paths when the local image pipeline is on.
     */
    public List<String> forExternalImport(Collection<String> images) {
        if (!externalImagesEnabled) {
            return List.of();
        }
        return filterImages(images).stream()
                .filter(this::isScrapedImagePath)
                .toList();
    }

    private boolean isAllowed(String value) {
        if (value == null || value.isBlank() || value.length() > 1000) {
            return false;
        }

        String normalized = normalize(value);
        if (normalized.startsWith(OWNER_IMAGE_PREFIX)) {
            return hasSafeSegments(normalized);
        }

        if (normalized.startsWith(SCRAPED_IMAGE_PREFIX)) {
            return externalImagesEnabled
                    && hasSafeSegments(normalized)
                    && scrapedFileExists(normalized);
        }

        return false;
    }

    private boolean isScrapedImagePath(String value) {
        return normalize(value).startsWith(SCRAPED_IMAGE_PREFIX);
    }

    private String normalize(String value) {
        String normalized = value.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("api/")) {
            normalized = normalized.substring(4);
        }
        return normalized;
    }

    private boolean hasSafeSegments(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains("..")
                || lower.contains("%2e")
                || lower.contains("%2f")
                || lower.contains("%5c")
                || lower.contains("%25")
                || value.contains("\\")) {
            return false;
        }

        String decoded;
        try {
            decoded = URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (decoded.contains("..") || decoded.contains("\\") || decoded.chars().anyMatch(Character::isISOControl)) {
            return false;
        }

        return decoded.startsWith(OWNER_IMAGE_PREFIX) || decoded.startsWith(SCRAPED_IMAGE_PREFIX);
    }

    private static Path resolveImageDir(String configured) {
        Path path = Path.of(configured).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            Path parentCandidate = Path.of("..", configured).toAbsolutePath().normalize();
            if (Files.exists(parentCandidate)) {
                return parentCandidate;
            }
        }
        return path;
    }

    private boolean scrapedFileExists(String normalized) {
        String relative = normalized.substring(SCRAPED_IMAGE_PREFIX.length());
        Path resolved = externalImageRoot.resolve(relative).normalize();
        return resolved.startsWith(externalImageRoot) && Files.isRegularFile(resolved);
    }
}
