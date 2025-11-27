package gr.hua.dit.dras.services;

/* imports */
import gr.hua.dit.dras.dto.ExternalListingDTO;
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.repositories.ListingRepository;
import gr.hua.dit.dras.repositories.OwnerRepository;
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

    public void importExternalListings(List<ExternalListingDTO> dtos) {
        Owner systemOwner = ownerRepository.findBySystemOwnerTrue()
                .orElseThrow(() -> new IllegalStateException("System owner not found!"));

        for (ExternalListingDTO dto : dtos) {
            Listing listing = new Listing();
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

            listingRepository.save(listing);
        }
    }
}
