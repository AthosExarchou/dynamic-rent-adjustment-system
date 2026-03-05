package gr.hua.dit.dras.services;

/* imports */
import gr.hua.dit.dras.dto.ExternalListingDTO;
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.model.enums.PropertyType;
import gr.hua.dit.dras.model.enums.RentalDuration;
import gr.hua.dit.dras.repositories.ListingRepository;
import gr.hua.dit.dras.repositories.OwnerRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class ExternalListingImportService {

    private final ListingRepository listingRepository;
    private final OwnerRepository ownerRepository;

    public ExternalListingImportService(ListingRepository listingRepository, OwnerRepository ownerRepository) {
        this.listingRepository = listingRepository;
        this.ownerRepository = ownerRepository;
    }

    /**
     * Imports or updates external listings.
     */
    @Transactional
    public void importExternalListings(List<ExternalListingDTO> dtos) {

        /* Retrieve the dedicated system owner for externally imported listings */
        Owner systemOwner = ownerRepository.findBySystemOwnerTrue()
                .orElseThrow(() -> new IllegalStateException("System owner not found!"));

        for (ExternalListingDTO dto : dtos) {

            /* Validates mandatory external data before processing */
            validateDto(dto);

            /* Reuses existing listing by source URL or creates a new one */
            Listing listing = listingRepository
                    .findBySourceUrl(dto.getSourceUrl())
                    .orElseGet(Listing::new);

            /* Maps basic listing fields from external DTO */
            listing.setTitle(dto.getTitle());
            listing.setSubtitle(dto.getSubtitle());
            listing.setDescription(dto.getDescription());
            listing.setPrice(dto.getPrice());
            listing.setPricePerM2(dto.getPricePerM2());
            listing.setAddress(dto.getAddress());
            listing.setSizeM2(dto.getSizeM2());
            listing.setRooms(dto.getRooms());
            listing.setPropertyType(mapPropertyType(dto.getPropertyType()));
            listing.setRentalDuration(mapRentalDuration(dto.getRentalDuration()));
            listing.setSourceUrl(dto.getSourceUrl());

            /* Marks as externally sourced and auto-approved */
            listing.setExternal(true);
            listing.setOwner(systemOwner);
            listing.setStatus(ListingStatus.APPROVED);
            listing.setDateScraped(dto.getDateScraped());

            /* Replaces images if valid image URLs are provided */
            if (dto.getImages() != null && !dto.getImages().isEmpty()) {
                listing.setImages(
                        dto.getImages().stream()
                                .filter(img -> img != null && !img.isBlank())
                                .toList()
                );
            }

            listingRepository.save(listing);
        }
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

    /**
     * Validates mandatory external listing fields.
     * Throws IllegalArgumentException if required data is missing.
     */
    private void validateDto(ExternalListingDTO dto) {
        if (dto.getSourceUrl() == null) {
            throw new IllegalArgumentException("Missing sourceUrl");
        }

        if (dto.getDateScraped() == null) {
            throw new IllegalArgumentException("Missing scrape timestamp for: " + dto.getSourceUrl());
        }

        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new IllegalArgumentException("Missing title for: " + dto.getSourceUrl());
        }
    }

}
