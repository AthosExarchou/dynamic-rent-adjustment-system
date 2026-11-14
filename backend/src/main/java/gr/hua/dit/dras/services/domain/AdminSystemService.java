package gr.hua.dit.dras.services.domain;

import gr.hua.dit.dras.dto.SystemStatusDTO;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.repositories.ListingRepository;
import gr.hua.dit.dras.repositories.OwnerRepository;
import gr.hua.dit.dras.repositories.TenantRepository;
import gr.hua.dit.dras.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AdminSystemService {

    private static final Logger log = LoggerFactory.getLogger(AdminSystemService.class);

    private final MacroAnalyticsService macroAnalyticsService;
    private final PriceSuggestionService priceSuggestionService;
    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final TenantRepository tenantRepository;
    private final ListingRepository listingRepository;

    private volatile Instant lastReloadedAt = Instant.now();

    public AdminSystemService(
            MacroAnalyticsService macroAnalyticsService,
            PriceSuggestionService priceSuggestionService,
            UserRepository userRepository,
            OwnerRepository ownerRepository,
            TenantRepository tenantRepository,
            ListingRepository listingRepository
    ) {
        this.macroAnalyticsService = macroAnalyticsService;
        this.priceSuggestionService = priceSuggestionService;
        this.userRepository = userRepository;
        this.ownerRepository = ownerRepository;
        this.tenantRepository = tenantRepository;
        this.listingRepository = listingRepository;
    }

    public SystemStatusDTO getSystemStatus() {
        File macroFile = findExistingFile("macro_predictions.json");
        String macroFilePath = null;
        String macroLastModified = "Not found";
        String macroAge = "N/A";

        if (macroFile != null) {
            macroFilePath = macroFile.getAbsolutePath();
            Instant instant = Instant.ofEpochMilli(macroFile.lastModified());
            macroLastModified = DateTimeFormatter.ISO_INSTANT.format(instant);
            macroAge = formatAge(instant);
        }

        boolean macroLoaded = false;
        try {
            macroLoaded = (macroAnalyticsService.getMacroForecast() != null);
        } catch (Exception e) {
            macroLoaded = false;
        }

        File priceFile = findExistingFile("price_predictions.json");
        String priceFilePath = null;
        String priceLastModified = "Not found";
        String priceAge = "N/A";
        boolean priceLoaded = false;

        if (priceFile != null) {
            priceFilePath = priceFile.getAbsolutePath();
            Instant instant = Instant.ofEpochMilli(priceFile.lastModified());
            priceLastModified = DateTimeFormatter.ISO_INSTANT.format(instant);
            priceAge = formatAge(instant);
            priceLoaded = priceFile.canRead();
        }

        String lastReloadedTimestamp = DateTimeFormatter.ISO_INSTANT.format(lastReloadedAt);
        String lastReloadedAge = formatAge(lastReloadedAt);

        long totalUsers = userRepository.count();
        long totalOwners = ownerRepository.count();
        long totalTenants = tenantRepository.count();

        long totalListings = listingRepository.count();
        long localListings = listingRepository.countByExternalFalse();
        long externalListings = listingRepository.countByExternalTrue();

        long activeListings = listingRepository.countByStatus(ListingStatus.APPROVED);
        long pendingListings = listingRepository.countByStatus(ListingStatus.PENDING);
        long rentedListings = listingRepository.countByStatus(ListingStatus.RENTED);
        long rejectedListings = listingRepository.countByStatus(ListingStatus.REJECTED);

        return new SystemStatusDTO(
                macroFilePath,
                macroLastModified,
                macroAge,
                macroLoaded,
                priceFilePath,
                priceLastModified,
                priceAge,
                priceLoaded,
                lastReloadedTimestamp,
                lastReloadedAge,
                totalUsers,
                totalOwners,
                totalTenants,
                totalListings,
                localListings,
                externalListings,
                activeListings,
                pendingListings,
                rentedListings,
                rejectedListings
        );
    }

    public void reloadForecastData() {
        log.info("Reloading macro and price forecast model data upon admin request.");
        macroAnalyticsService.loadModelData();
        priceSuggestionService.loadModelData();
        this.lastReloadedAt = Instant.now();
    }

    private File findExistingFile(String filename) {
        List<Path> candidatePaths = List.of(
                Paths.get("analytics", "outputs", filename),
                Paths.get("..", "analytics", "outputs", filename),
                Paths.get("outputs", filename)
        );
        for (Path path : candidatePaths) {
            File file = path.toFile();
            if (file.exists() && file.canRead()) {
                return file;
            }
        }
        return null;
    }

    private String formatAge(Instant fileInstant) {
        if (fileInstant == null) {
            return "N/A";
        }
        Duration duration = Duration.between(fileInstant, Instant.now());
        long seconds = duration.getSeconds();
        if (seconds < 0) {
            return "just now";
        }
        long days = duration.toDays();
        if (days > 0) {
            return days == 1 ? "1 day ago" : days + " days ago";
        }
        long hours = duration.toHours();
        if (hours > 0) {
            return hours == 1 ? "1 hour ago" : hours + " hours ago";
        }
        long minutes = duration.toMinutes();
        if (minutes > 0) {
            return minutes == 1 ? "1 minute ago" : minutes + " minutes ago";
        }
        return "just now";
    }
}
