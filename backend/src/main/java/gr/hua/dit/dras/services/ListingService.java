package gr.hua.dit.dras.services;

/* imports */
import gr.hua.dit.dras.entities.*;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.repositories.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

    @Transactional
    public List<Listing> filterListings(String title, int minPrice, int maxPrice) {

        if (title == null || title.isBlank()) {
            return listingRepository.findByPriceBetween(minPrice, maxPrice);
        }
        return listingRepository.findByTitleContainingIgnoreCaseAndPriceBetween(title, minPrice, maxPrice);
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
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ADMIN"));

        if (isAdmin) {
            return;
        }

        if (!currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("OWNER"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Owner owner = listing.getOwner();

        if (owner == null ||
                owner.getUser() == null ||
                !owner.getUser().getId().equals(currentUser.getId())) {

            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

}
