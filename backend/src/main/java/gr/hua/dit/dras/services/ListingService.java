package gr.hua.dit.dras.services;

/* imports */
import gr.hua.dit.dras.entities.*;
import gr.hua.dit.dras.repositories.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import jakarta.servlet.http.HttpSession;

@Service
public class ListingService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private ListingRepository listingRepository;
    private TenantRepository tenantRepository;
    private OwnerRepository ownerRepository;

    public ListingService(RoleRepository roleRepository, UserRepository userRepository, UserService userService, ListingRepository listingRepository, TenantRepository tenantRepository, OwnerRepository ownerRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.listingRepository = listingRepository;
        this.tenantRepository = tenantRepository;
        this.ownerRepository = ownerRepository;
    }

    @Transactional
    public List<Listing> getListings() {
        return listingRepository.findAll();
    }

    public List<Listing> getListingsByOwner(Owner owner) {
        return listingRepository.findByOwner(owner);
    }

    @Transactional
    public void saveListing(Listing listing) {
        listingRepository.save(listing);
    }

    @Transactional
    public Listing getListing(Integer listingId) {

        return listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Listing with id " + listingId + " not found"
                ));
    }

    @Transactional
    public void deleteListing(Integer listingId) {

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + listingId));

        /* unassigns tenant */
        Tenant tenant = listing.getTenant();
        if (tenant != null) {
            unassignTenantFromListing(listingId, tenant.getId());
        }

        /* unassigns owner */
        Owner owner = listing.getOwner();
        if (owner != null) {
            unassignOwnerFromListing(listingId);
        }

        /* deletes listing */
        listingRepository.delete(listing);
    }

    @Transactional
    public void assignOwnerToListing(Integer listingId, Owner owner) {

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));

        listing.setOwner(owner);

        Integer currentUserId = userService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Role ownerRole = roleRepository.findByName("OWNER")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        if (!currentUser.getRoles().contains(ownerRole)) {
            currentUser.getRoles().add(ownerRole);
            userService.updateUser(currentUser); //saves the user
        }
        listingRepository.save(listing);
    }

    @Transactional
    public void unassignOwnerFromListing(Integer listingId) {

        Listing listing = listingRepository.findById(listingId).get();
        listing.setOwner(null);
        listingRepository.save(listing);
    }

    @Transactional
    public void assignTenantToListing(Integer listingId, Tenant tenant, String RoleUserIs) {

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));

        listing.setTenant(tenant);

        Integer currentUserId = userService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Role tenantRole = roleRepository.findByName("TENANT")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        Role ownerRole = roleRepository.findByName("OWNER")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        if (!RoleUserIs.equals("owner")) {
            currentUser.getRoles().add(tenantRole);
            userService.updateUser(currentUser); //saves the user
        }
        listingRepository.save(listing);
    }

    @Transactional
    public void unassignTenantFromListing(Integer listingId, Integer tenantId) {

        Listing listing = listingRepository.findById(listingId).get();
        Tenant tenant = tenantRepository.findById(tenantId).get();
        listing.setTenant(null); //unlinks tenant from listing
        tenant.setAppliedListings(null);
        tenantRepository.save(tenant);
        listingRepository.save(listing);
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
            Optional<Role> optionalRole = roleRepository.findByName("OWNER");

            if (optionalRole.isPresent()) {
                Role ownerRole = optionalRole.get();
                System.out.println("User roles before adding: " + user.getRoles());

                if (!user.getRoles().contains(ownerRole)) {
                    user.getRoles().add(ownerRole);
                }
                userService.updateUser(user);
                session.invalidate();
                System.out.println("User roles after adding: " + user.getRoles());
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Role 'OWNER' does not exist in the database.");
            }
        }
    }

    @Transactional
    public List<Listing> filterListings(String title, int minPrice, int maxPrice) {

        if (title == null || title.isBlank()) {
            return listingRepository.findByPriceBetween(minPrice, maxPrice);
        }
        return listingRepository.findByTitleContainingIgnoreCaseAndPriceBetween(title, minPrice, maxPrice);
    }

}

