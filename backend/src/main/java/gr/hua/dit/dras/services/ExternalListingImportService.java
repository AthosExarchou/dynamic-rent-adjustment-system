package gr.hua.dit.dras.services;

/* imports */
import gr.hua.dit.dras.dto.ExternalListingDTO;
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.repositories.ListingRepository;
import gr.hua.dit.dras.repositories.OwnerRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ExternalListingImportService {

    private final ListingRepository listingRepository;
    private final OwnerRepository ownerRepository;

    public ExternalListingImportService(ListingRepository listingRepository, OwnerRepository ownerRepository) {
        this.listingRepository = listingRepository;
        this.ownerRepository = ownerRepository;
    }

    @Transactional
    public void importExternalListings(List<ExternalListingDTO> dtos) {
        Owner systemOwner = ownerRepository.findBySystemOwnerTrue()
                .orElseThrow(() -> new IllegalStateException("System owner not found!"));

        for (ExternalListingDTO dto : dtos) {

            validateDto(dto);

            Listing listing = listingRepository
                    .findBySourceUrl(dto.getSourceUrl())
                    .orElseGet(Listing::new);

            listing.setTitle(dto.getTitle());
            listing.setSubtitle(dto.getSubtitle());
            listing.setDescription(dto.getDescription());
            listing.setPrice(dto.getPrice());
            listing.setPricePerM2(dto.getPricePerM2());
            listing.setAddress(dto.getAddress());
            listing.setSizeM2(dto.getSizeM2());
            listing.setRooms(dto.getRooms());
            listing.setPropertyType(dto.getPropertyType());
            listing.setRentalDuration(dto.getRentalDuration());
            listing.setSourceUrl(dto.getSourceUrl());

            listing.setExternal(true);
            listing.setOwner(systemOwner);
            listing.setStatus(ListingStatus.APPROVED);
            listing.setDateScraped(dto.getDateScraped());

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
