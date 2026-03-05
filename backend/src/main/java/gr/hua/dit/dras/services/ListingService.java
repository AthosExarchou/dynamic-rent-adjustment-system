package gr.hua.dit.dras.services;

/* imports */
import gr.hua.dit.dras.dto.ListingFilterDTO;
import gr.hua.dit.dras.entities.*;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.repositories.*;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpSession;

@Service
@Transactional
public class ListingService {

    private final RoleRepository roleRepository;
    private final UserService userService;
    private final ListingRepository listingRepository;
    private final OwnerService ownerService;
    private final TenantService tenantService;

    public ListingService(
            RoleRepository roleRepository,
            UserService userService,
            ListingRepository listingRepository,
            OwnerService ownerService,
            TenantService tenantService
    ) {
        this.roleRepository = roleRepository;
        this.userService = userService;
        this.listingRepository = listingRepository;
        this.ownerService = ownerService;
        this.tenantService = tenantService;
    }

    @Transactional
    public List<Listing> getListings() {
        return listingRepository.findAll();
    }

    @Transactional
    public List<Listing> getLocalListings() {
        return listingRepository.findByExternalFalse();
    }

    @Transactional
    public List<Listing> getListingsByOwner(Owner owner) {
        return listingRepository.findByOwner(owner);
    }

    @Transactional
    public Listing getListing(Integer listingId) {

        return listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Listing with id " + listingId + " not found"
                ));
    }

    @Transactional
    public List<Listing> getPendingListings() {
        return listingRepository.findByStatus(ListingStatus.PENDING);
    }

    @Transactional
    public void saveListing(Listing listing) {
        /* Enforces required fields for external listings */
        if (listing.isExternal()) {
            if (listing.getDateScraped() == null) {
                listing.setDateScraped(Instant.now());
            }
        } else {
            listing.setDateScraped(null); //local listing
        }

        listingRepository.save(listing);
    }

    @Transactional
    public void deleteListing(Integer listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + listingId));

        /* External listings cannot be deleted manually */
        if (listing.isExternal()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
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
    public boolean isFirstListing(Owner owner) {

        /* checks if the owner already has listings */
        List<Listing> listings = getListingsByOwner(owner);
        return listings.isEmpty(); //if no listings found, it's the first one
    }

    @Transactional
    public void assignRoleToUserForFirstListing(Owner owner, HttpSession session) {

        if (isFirstListing(owner)) {
            User user = owner.getUser();
            Role ownerRole = roleRepository.findByName("OWNER")
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Role 'OWNER' does not exist in the database"
                    ));

            System.out.println("User roles before adding: " + user.getRoles());

            if (!user.getRoles().contains(ownerRole)) {
                user.getRoles().add(ownerRole);
            }
            userService.updateUser(user);
            session.invalidate();
            System.out.println("User roles after adding: " + user.getRoles());
        }
    }

    /**
     * Builds a dynamic JPA Specification based on the provided filter
     * and returns all matching listings.
     */
    public List<Listing> filterListings(ListingFilterDTO filter) {

        /* Validates numeric and date ranges before building the query */
        validateRanges(filter);

        Specification<Listing> spec = Specification.where(null); //start with an empty specification

        /* Case-insensitive partial match on title */
        if (hasText(filter.getTitle())) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("title")),
                            "%" + filter.getTitle().trim().toLowerCase() + "%"));
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
                    cb.equal(root.get("type"), filter.getType()));
        }

        /* Case-insensitive exact match on municipality */
        if (hasText(filter.getMunicipality())) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("municipality")),
                            filter.getMunicipality().trim().toLowerCase()));
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

        /* Last updated date range filters */
        if (filter.getUpdatedAfter() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("updatedAt"),
                            filter.getUpdatedAfter()));
        }

        if (filter.getUpdatedBefore() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("updatedAt"),
                            filter.getUpdatedBefore()));
        }

        /* Includes only externally sourced listings */
        if (Boolean.TRUE.equals(filter.getExternalOnly())) {
            spec = spec.and((root, query, cb) ->
                    cb.isTrue(root.get("external")));
        }

        return listingRepository.findAll(spec);
    }

    /**
     * Returns true if the string is non-null and contains non-whitespace text.
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Validates that provided numeric and date ranges are logically correct.
     * Throws IllegalArgumentException if a range is invalid.
     */
    private void validateRanges(ListingFilterDTO filter) {
        if (filter.getMinPrice() != null &&
                filter.getMaxPrice() != null &&
                filter.getMinPrice() > filter.getMaxPrice()) {
            throw new IllegalArgumentException("Invalid price range.");
        }

        if (filter.getUpdatedAfter() != null &&
                filter.getUpdatedBefore() != null &&
                filter.getUpdatedAfter().isAfter(filter.getUpdatedBefore())) {
            throw new IllegalArgumentException("Invalid date range.");
        }
    }

    @Transactional
    public void markAsRented(Listing listing) {
        listing.setStatus(ListingStatus.RENTED);
        listingRepository.save(listing);
    }

    @Transactional
    public void makeAvailable(Listing listing) {
        listing.setStatus(ListingStatus.APPROVED);
        listingRepository.save(listing);
    }

    @Transactional
    public void approveListing(Integer listingId) {

        Listing listing = getListing(listingId);
        listing.approve();

        listingRepository.save(listing);
    }

    @Transactional
    public void cleanupExternalListings(int graceDays) {
        Instant cutoff = Instant.now().minus(graceDays, ChronoUnit.DAYS);

        List<Listing> externalListings = listingRepository.findByExternalTrue();

        for (Listing listing : externalListings) {
            /* Deletes if last scraped date is older than cutoff */
            if (listing.getDateScraped() != null && listing.getDateScraped().isBefore(cutoff)) {
                listingRepository.delete(listing);
            }
        }
    }

    public void validateListingModificationRights(Listing listing, User currentUser) {

        if (currentUser == null) {
            throw new AccessDeniedException("Unauthenticated");
        }

        Set<String> roleNames = currentUser.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        boolean isAdmin = roleNames.contains("ADMIN");
        boolean isOwner = roleNames.contains("OWNER");

        if (isAdmin) {
            return; //admins bypass all checks
        }

        if (!isOwner) {
            throw new AccessDeniedException("Not an owner");
        }

        Owner owner = listing.getOwner();
        if (owner == null || owner.getUser() == null ||
                !owner.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Not owner of this listing");
        }

        /* Prevents modification if listing is rented */
        if (listing.isRented()) {
            throw new IllegalStateException("Cannot modify a rented listing");
        }
    }

}
