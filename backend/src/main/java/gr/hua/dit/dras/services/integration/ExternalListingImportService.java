package gr.hua.dit.dras.services.integration;

/* imports */
import gr.hua.dit.dras.dto.ExternalListingDTO;
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.model.enums.PropertyType;
import gr.hua.dit.dras.model.enums.RentalDuration;
import gr.hua.dit.dras.repositories.ListingRepository;
import gr.hua.dit.dras.repositories.OwnerRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExternalListingImportService {

    private static final Logger log = LoggerFactory.getLogger(ExternalListingImportService.class);

    private final ListingRepository listingRepository;
    private final OwnerRepository ownerRepository;
    private final Validator validator;

    public ExternalListingImportService(
            ListingRepository listingRepository,
            OwnerRepository ownerRepository,
            Validator validator
    ) {
        this.listingRepository = listingRepository;
        this.ownerRepository = ownerRepository;
        this.validator = validator;
    }

    /**
     * Imports or updates external listings.
     */
    @Transactional
    public int importExternalListings(List<ExternalListingDTO> dtos) {

        /* Retrieves the dedicated system owner for externally imported listings */
        Owner systemOwner = ownerRepository.findBySystemOwnerTrue()
                .orElseThrow(() -> new IllegalStateException("System owner not found!"));

        List<Listing> listingsToSave = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (ExternalListingDTO dto : dtos) {
            try {
                /* Validates mandatory external data before processing */
                Set<ConstraintViolation<ExternalListingDTO>> violations = validator.validate(dto);
                if (!violations.isEmpty()) {
                    String msg = violations.stream()
                            .map(ConstraintViolation::getMessage)
                            .collect(Collectors.joining("; "));
                    throw new IllegalArgumentException(msg);
                }

                /* Reuses existing listing by source URL or creates a new one */
                Listing listing = listingRepository
                        .findBySourceUrl(dto.getSourceUrl())
                        .orElseGet(Listing::new);

                /* Maps basic listing fields from external DTO */
                listing.setTitle(dto.getTitle());
                listing.setSubtitle(dto.getSubtitle());
                listing.setDescription(dto.getDescription());
                listing.setAddress(dto.getAddress());
                listing.setSourceUrl(dto.getSourceUrl());

                /* Bounds numeric values to prevent JPA Constraint Exceptions */
                listing.setPrice(Math.max(0, Math.min(dto.getPrice(), 20000)));
                listing.setSizeM2(Math.max(5, Math.min(dto.getSizeM2(), 1000)));
                listing.setRooms(
                        Math.max(1, Math.min(dto.getRooms() != null ? dto.getRooms() : 1, 20))
                ); // caps at entity @Max(20)

                /* Calculates pricePerM2 if missing, and enforces the 0-200 boundary */
                int calculatedPricePerM2 = dto.getPricePerM2() != null
                        ? dto.getPricePerM2()
                        : (listing.getSizeM2() > 0 ? (listing.getPrice() / listing.getSizeM2()) : 0);

                listing.setPricePerM2(Math.max(0, Math.min(calculatedPricePerM2, 200)));

                listing.setPropertyType(mapPropertyType(dto.getPropertyType()));
                listing.setRentalDuration(mapRentalDuration(dto.getRentalDuration()));

                /* Marks as externally sourced and auto-approved */
                listing.setExternal(true);
                listing.setOwner(systemOwner);
                listing.setStatus(ListingStatus.APPROVED);
                listing.setDateScraped(Instant.now());

                /* Replaces images if valid image URLs are provided */
                if (dto.getImages() != null && !dto.getImages().isEmpty()) {
                    listing.setImages(
                            dto.getImages().stream()
                                    .filter(img -> img != null && !img.isBlank())
                                    .toList()
                    );
                }

                listingsToSave.add(listing);
                successCount++;
            } catch (Exception e) {
                log.warn("Skipping invalid listing [{}]: {}", dto.getSourceUrl(), e.getMessage());
                failCount++;
            }
        }
        listingRepository.saveAll(listingsToSave);

        log.info("Import complete. Success: {}, Failed: {}", successCount, failCount);
        return successCount;
    }

    /**
     * Maps raw external rental duration text to internal enum representation.
     */
    private RentalDuration mapRentalDuration(String raw) {
        if (raw == null)
            return RentalDuration.OTHER;

        return switch (raw.trim().toLowerCase()) {
            case "απεριόριστη" -> RentalDuration.INDEFINITE;
            default -> RentalDuration.OTHER;
        };
    }

    /**
     * Keyword mapping used to infer PropertyType from external text.
     */
    private static final Map<PropertyType, List<String>> PROPERTY_KEYWORDS = Map.of(
            PropertyType.APARTMENT, List.of("διαμ"),
            PropertyType.HOUSE, List.of("μονοκατοικ"),
            PropertyType.STUDIO, List.of("στούντιο", "γκαρσονιέρα"),
            PropertyType.MAISONETTE, List.of("μεζον"),
            PropertyType.LOFT, List.of("λοφτ"),
            PropertyType.VILLA, List.of("βίλα")
    );

    /**
     * Maps raw external property type text to internal enum using keyword matching.
     * Defaults to OTHER if no match is found.
     */
    private PropertyType mapPropertyType(String raw) {
        if (raw == null || raw.isBlank()) {
            return PropertyType.OTHER;
        }

        String normalized = raw.trim().toLowerCase();

        return PROPERTY_KEYWORDS.entrySet().stream()
                .filter(entry ->
                        entry.getValue().stream()
                                .anyMatch(normalized::contains)
                )
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(PropertyType.OTHER);
    }

}
