package gr.hua.dit.dras.services.domain;

/* imports */
import gr.hua.dit.dras.dto.ListingFilterDTO;
import gr.hua.dit.dras.entities.*;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.repositories.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static gr.hua.dit.dras.model.enums.ListingStatus.APPROVED;

@Service
@Transactional
public class ListingService {

    private final ListingRepository listingRepository;
    private final OwnerService ownerService;
    private final TenantService tenantService;

    public ListingService(
            ListingRepository listingRepository,
            OwnerService ownerService,
            TenantService tenantService
    ) {
        this.listingRepository = listingRepository;
        this.ownerService = ownerService;
        this.tenantService = tenantService;
    }

    @Transactional(readOnly = true)
    public List<Listing> getListings() {
        return listingRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Listing> getLocalListings() {
        return listingRepository.findByExternalFalse();
    }

    @Transactional(readOnly = true)
    public List<Listing> getListingsByOwner(Owner owner) {
        return listingRepository.findByOwner(owner);
    }

    @Transactional(readOnly = true)
    public Listing getListing(Integer listingId) {

        return listingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Listing with id " + listingId + " not found"
                ));
    }

    @Transactional(readOnly = true)
    public List<Listing> getPendingListings() {
        return listingRepository.findByStatus(ListingStatus.PENDING);
    }

    @Transactional
    public void saveListing(Listing listing) {
        /* Enforces required fields for external listings */
        if (listing.isExternal() && listing.getDateScraped() == null) {
            listing.setDateScraped(Instant.now());
        } else if (!listing.isExternal()) {
            listing.setDateScraped(null); // local listing
        }

        listingRepository.save(listing);
    }

    @Transactional
    public Listing updateListing(Listing listing) {
        return listingRepository.save(listing);
    }

    @Transactional
    public void deleteListing(Integer listingId) {

        Listing listing = getListing(listingId);

        /* External listings cannot be deleted manually */
        if (listing.isExternal()) {
            throw new IllegalStateException(
                    "External listings cannot be deleted manually. Use scheduled cleanup.");
        }

        /* Unassign tenant */
        Tenant tenant = listing.getTenant();
        if (tenant != null) {
            tenantService.unassignTenantFromListing(listingId, tenant.getId());
        }

        /* Unassign owner */
        Owner owner = listing.getOwner();
        if (owner != null) {
            ownerService.unassignOwnerFromListing(listingId);
        }

        listingRepository.delete(listing);
    }

    @Transactional
    public void deleteAllListingsForOwner(Owner owner) {
        /* Create a copy of the list to prevent ConcurrentModificationException during deletion */
        List<Listing> listings = new ArrayList<>(owner.getListings());

        for (Listing listing : listings) {
            if (!listing.isExternal()) {
                deleteListing(listing.getId());
            }
        }
    }

    @Transactional(readOnly = true)
    public boolean isFirstListing(Owner owner) {

        /* Checks if the owner already has listings */
        List<Listing> listings = getListingsByOwner(owner);
        return listings.isEmpty(); // if no listings found, it's the first one
    }

    /**
     * Builds a dynamic JPA Specification based on the provided filter
     * and returns all matching listings.
     */
    @Transactional(readOnly = true)
    public List<Listing> filterListings(ListingFilterDTO filter) {

        /* Validates numeric and date ranges before building the query */
        validateRanges(filter);

        Specification<Listing> spec = Specification.allOf(); // starts with an empty specification

        /* Keyword search matching title, subtitle, address, or description */
        if (hasText(filter.getSearch())) {
            String[] tokens = filter.getSearch().trim().toLowerCase().split("\\s+");
            for (String token : tokens) {
                if (!token.isEmpty()) {
                    String pattern = "%" + token + "%";
                    spec = spec.and((root, query, cb) -> cb.or(
                            cb.like(cb.lower(root.get("title")), pattern),
                            cb.and(cb.isNotNull(root.get("subtitle")), cb.like(cb.lower(root.get("subtitle")), pattern)),
                            cb.and(cb.isNotNull(root.get("address")), cb.like(cb.lower(root.get("address")), pattern)),
                            cb.and(cb.isNotNull(root.get("description")), cb.like(cb.lower(root.get("description")), pattern))
                    ));
                }
            }
        } else if (hasText(filter.getTitle())) {
            /* Case-insensitive partial match on title, subtitle, or address */
            String[] tokens = filter.getTitle().trim().toLowerCase().split("\\s+");
            for (String token : tokens) {
                if (!token.isEmpty()) {
                    String pattern = "%" + token + "%";
                    spec = spec.and((root, query, cb) -> cb.or(
                            cb.like(cb.lower(root.get("title")), pattern),
                            cb.and(cb.isNotNull(root.get("subtitle")), cb.like(cb.lower(root.get("subtitle")), pattern)),
                            cb.and(cb.isNotNull(root.get("address")), cb.like(cb.lower(root.get("address")), pattern))
                    ));
                }
            }
        }

        /* Price range filters */
        if (filter.getMinPrice() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("price"),
                            filter.getMinPrice()));
        }

        if (filter.getMaxPrice() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("price"),
                            filter.getMaxPrice()));
        }

        /* Exact match filters */
        if (filter.getType() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("propertyType"), filter.getType()));
        }

        /* Case-insensitive partial match on address (municipality) */
        if (hasText(filter.getMunicipality())) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("address")),
                            "%" + filter.getMunicipality().trim().toLowerCase() + "%"));
        }

        /* Case-insensitive partial match on district (address or subtitle) */
        if (hasText(filter.getDistrict())) {
            String term = "%" + filter.getDistrict().trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("address")), term),
                    cb.and(cb.isNotNull(root.get("subtitle")), cb.like(cb.lower(root.get("subtitle")), term))
            ));
        }

        /* Bedroom range filters */
        if (filter.getMinBedrooms() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("bedrooms"),
                            filter.getMinBedrooms()));
        }

        if (filter.getMaxBedrooms() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("bedrooms"),
                            filter.getMaxBedrooms()));
        }

        /* Bathroom range filters */
        if (filter.getMinBathrooms() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("bathrooms"),
                            filter.getMinBathrooms()));
        }

        if (filter.getMaxBathrooms() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("bathrooms"),
                            filter.getMaxBathrooms()));
        }

        /* Year built range filters */
        if (filter.getMinYear() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("yearBuilt"),
                            filter.getMinYear()));
        }

        if (filter.getMaxYear() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("yearBuilt"),
                            filter.getMaxYear()));
        }

        /* Last updated date range filters */
        if (filter.getUpdatedAfter() != null) {
            Instant from = filter.getUpdatedAfter()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant();
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("updatedAt"), from));
        }

        if (filter.getUpdatedBefore() != null) {
            Instant to = filter.getUpdatedBefore()
                    .plusDays(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant();
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("updatedAt"), to));
        }

        /* Filter by origin (externally scraped vs directly posted) */
        if (filter.getExternalOnly() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("external"), filter.getExternalOnly()));
        }

        /* Enforce APPROVED status for publicly filtered listings */
        spec = spec.and((root, query, cb) ->
                cb.equal(root.get("status"), APPROVED));

        return listingRepository.findAll(spec);
    }

    /**
     * Returns true if the string is non-null and contains non-whitespace text.
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Validates that provided numeric and date ranges are logically correct and adhere to listing constraints.
     * Throws IllegalArgumentException if a range or constraint is violated.
     */
    private void validateRanges(ListingFilterDTO filter) {
        LocalDate today = LocalDate.now();
        int maxYear = Year.now().getValue() + 5;

        // Price constraints
        if (filter.getMinPrice() != null && (filter.getMinPrice() < 0 || filter.getMinPrice() > 20000)) {
            throw new IllegalArgumentException("Minimum price must be between 0 and 20000.");
        }
        if (filter.getMaxPrice() != null && (filter.getMaxPrice() < 0 || filter.getMaxPrice() > 20000)) {
            throw new IllegalArgumentException("Maximum price must be between 0 and 20000.");
        }
        if (filter.getMinPrice() != null &&
                filter.getMaxPrice() != null &&
                filter.getMinPrice() > filter.getMaxPrice()) {
            throw new IllegalArgumentException("Invalid price range.");
        }

        // Bedroom constraints
        if (filter.getMinBedrooms() != null && (filter.getMinBedrooms() < 0 || filter.getMinBedrooms() > 10)) {
            throw new IllegalArgumentException("Bedrooms must be between 0 and 10.");
        }
        if (filter.getMaxBedrooms() != null && (filter.getMaxBedrooms() < 0 || filter.getMaxBedrooms() > 10)) {
            throw new IllegalArgumentException("Bedrooms must be between 0 and 10.");
        }
        if (filter.getMinBedrooms() != null &&
                filter.getMaxBedrooms() != null &&
                filter.getMinBedrooms() > filter.getMaxBedrooms()) {
            throw new IllegalArgumentException("Invalid bedroom range.");
        }

        // Bathroom constraints (0 - 5)
        if (filter.getMinBathrooms() != null && (filter.getMinBathrooms() < 0 || filter.getMinBathrooms() > 5)) {
            throw new IllegalArgumentException("Bathrooms must be between 0 and 5.");
        }
        if (filter.getMaxBathrooms() != null && (filter.getMaxBathrooms() < 0 || filter.getMaxBathrooms() > 5)) {
            throw new IllegalArgumentException("Bathrooms must be between 0 and 5.");
        }
        if (filter.getMinBathrooms() != null &&
                filter.getMaxBathrooms() != null &&
                filter.getMinBathrooms() > filter.getMaxBathrooms()) {
            throw new IllegalArgumentException("Invalid bathroom range.");
        }

        // Year built constraints (1800 - currentYear + 5)
        if (filter.getMinYear() != null && (filter.getMinYear() < 1800 || filter.getMinYear() > maxYear)) {
            throw new IllegalArgumentException("Year built must be between 1800 and " + maxYear + ".");
        }
        if (filter.getMaxYear() != null && (filter.getMaxYear() < 1800 || filter.getMaxYear() > maxYear)) {
            throw new IllegalArgumentException("Year built must be between 1800 and " + maxYear + ".");
        }
        if (filter.getMinYear() != null &&
                filter.getMaxYear() != null &&
                filter.getMinYear() > filter.getMaxYear()) {
            throw new IllegalArgumentException("Invalid year built range.");
        }

        // Date constraints (cannot be in future, and updatedAfter must be before updatedBefore)
        if (filter.getUpdatedAfter() != null && filter.getUpdatedAfter().isAfter(today)) {
            throw new IllegalArgumentException("Updated After date cannot be in the future.");
        }
        if (filter.getUpdatedBefore() != null && filter.getUpdatedBefore().isAfter(today)) {
            throw new IllegalArgumentException("Updated Before date cannot be in the future.");
        }
        if (filter.getUpdatedAfter() != null &&
                filter.getUpdatedBefore() != null &&
                !filter.getUpdatedAfter().isBefore(filter.getUpdatedBefore())) {
            throw new IllegalArgumentException("Invalid date range: Updated After must be earlier than Updated Before.");
        }
    }

    @Transactional
    public void approveListing(Integer listingId) {

        Listing listing = getListing(listingId);
        listing.approve();

        listingRepository.save(listing);
    }

    @Transactional
    public Listing rejectListing(Integer listingId, String reason) {

        Listing listing = getListing(listingId);
        listing.reject(reason);

        return listingRepository.save(listing);
    }

    @Transactional
    public Listing rejectListing(Integer listingId) {
        return rejectListing(listingId, "Listing does not meet publication criteria");
    }

    @Transactional
    public void disableListing(Integer listingId) {

        Listing listing = getListing(listingId);
        listing.disable();

        listingRepository.save(listing);
    }

    @Transactional
    public void enableListing(Integer listingId) {

        Listing listing = getListing(listingId);
        listing.enable();

        listingRepository.save(listing);
    }

    @Transactional
    public void cleanupExternalListings(int graceDays) {
        /* Deletes if last scraped date is older than cutoff */
        Instant cutoff = Instant.now().minus(graceDays, ChronoUnit.DAYS);
        listingRepository.deleteByExternalTrueAndDateScrapedBefore(cutoff);

    }

    public void validateListingOwnership(Listing listing, User currentUser) {

        if (currentUser == null) {
            throw new AccessDeniedException("Unauthenticated");
        }

        Set<String> roleNames = currentUser.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        if (roleNames.contains("ADMIN")) {
            return; // admins bypass all checks
        }

        if (!roleNames.contains("OWNER")) {
            throw new AccessDeniedException("Not an owner");
        }

        Owner owner = listing.getOwner();
        if (owner == null || owner.getUser() == null ||
                !owner.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Not owner of this listing");
        }
    }

    public void validateListingModificationRights(Listing listing, User currentUser) {

        validateListingOwnership(listing, currentUser);

        /* Prevents modification if listing is rented */
        if (listing.isRented()) {
            throw new IllegalStateException("Cannot modify a rented listing");
        }
    }

    @Transactional
    public void rejectApplicant(Listing listing, Tenant tenant) {
        if (!listing.getApplicants().contains(tenant)) {
            throw new IllegalStateException("Tenant did not apply for this listing");
        }

        listing.removeApplicant(tenant);

        /* Makes sure this tenant was not assigned as the renter */
        if (listing.getTenant() != null && listing.getTenant().equals(tenant)) {
            listing.setTenant(null);
            listing.setStatus(APPROVED); // listing becomes available again
        }

        listingRepository.save(listing);
    }

}
