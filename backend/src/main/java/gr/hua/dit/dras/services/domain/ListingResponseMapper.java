package gr.hua.dit.dras.services.domain;

/* imports */
import gr.hua.dit.dras.entities.Listing;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds listing API responses and applies the image policy on the read side.
 */
@Component
public class ListingResponseMapper {

    private final ListingImagePolicy imagePolicy;

    public ListingResponseMapper(ListingImagePolicy imagePolicy) {
        this.imagePolicy = imagePolicy;
    }

    public Map<String, Object> map(Listing l) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", l.getId());
        map.put("title", l.getTitle());
        map.put("subtitle", l.getSubtitle() != null ? l.getSubtitle() : "");
        map.put("description", l.getDescription());
        map.put("price", l.getPrice());
        map.put("pricePerM2", l.getPricePerM2());
        map.put("address", l.getAddress());
        map.put("propertyType", l.getPropertyType() != null ? l.getPropertyType().name() : null);
        map.put("rentalDuration", l.getRentalDuration() != null ? l.getRentalDuration().name() : null);
        map.put("yearBuilt", l.getYearBuilt());
        map.put("sizeM2", l.getSizeM2());
        map.put("floor", l.getFloor());
        map.put("bedrooms", l.getBedrooms());
        map.put("bathrooms", l.getBathrooms());
        map.put("images", imagePolicy.forListing(l));
        map.put("status", l.getStatus().name());
        map.put("rented", l.isRented());
        map.put("rejectionReason", l.getRejectionReason());
        if (l.getOwner() != null) {
            Map<String, Object> ownerMap = new HashMap<>();
            ownerMap.put("id", l.getOwner().getId());
            if (l.getOwner().getUser() != null) {
                ownerMap.put("userId", l.getOwner().getUser().getId());
            }
            map.put("owner", ownerMap);
        }
        return map;
    }
}
