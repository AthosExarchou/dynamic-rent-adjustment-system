package gr.hua.dit.dras.services.domain;

/* imports */
import gr.hua.dit.dras.entities.*;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.model.enums.RentalStatus;
import gr.hua.dit.dras.repositories.TenantRepository;
import gr.hua.dit.dras.repositories.ListingRepository;
import gr.hua.dit.dras.repositories.RoleRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TenantService {

    private final TenantRepository tenantRepository;
    private final ListingRepository listingRepository;
    private final UserService userService;
    private final RoleRepository roleRepository;

    public TenantService(
            TenantRepository tenantRepository,
            ListingRepository listingRepository,
            UserService userService,
            RoleRepository roleRepository
    ) {
        this.tenantRepository = tenantRepository;
        this.listingRepository = listingRepository;
        this.userService = userService;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<Tenant> getTenants() {
        return tenantRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Tenant> findTenantByUserId(Integer userId) {
        return tenantRepository.findByUserId(userId);
    }

    @Transactional
    public void saveTenant(Tenant tenant) {
        tenantRepository.save(tenant);
    }

    @Transactional(readOnly = true)
    public Tenant getTenant(Integer tenantId) {

        if (tenantId != null) {
            return tenantRepository.findById(tenantId)
                    .orElseThrow(() ->
                            new IllegalStateException("Tenant not found with id: " + tenantId)
                    );
        }

        Integer currentUserId = userService.getCurrentUserId();

        return tenantRepository.findByUserId(currentUserId)
                .orElseThrow(() ->
                        new IllegalStateException("Tenant profile not found for current user")
                );
    }

    @Transactional
    public Tenant createTenantForUser(
            Integer userId,
            String firstName,
            String lastName,
            String phoneNumber
    ) {

        User user = userService.getUser(userId); // fetches user by ID

        tenantRepository.findByUserId(userId).ifPresent(t -> {
            throw new IllegalStateException(
                    "User already has a Tenant profile"
            );
        });

        Tenant tenant = new Tenant();
        tenant.setFirstName(firstName);
        tenant.setLastName(lastName);
        tenant.setPhoneNumber(phoneNumber);
        tenant.setRentalStatus(RentalStatus.APPLIED);
        tenant.setUser(user); // associates tenant with the user

        Tenant savedTenant = tenantRepository.save(tenant);

        assignTenantRole(user); // assigns role 'TENANT'

        return savedTenant;
    }

    @Transactional
    public void createTenantForCurrentUser(
            String firstName,
            String lastName,
            String phoneNumber
    ) {
        Integer userId = userService.getCurrentUserId();
        createTenantForUser(userId, firstName, lastName, phoneNumber);
    }

    @Transactional(readOnly = true)
    public Integer getTenantIdForCurrentUser() {

        Integer userId = userService.getCurrentUserId();

        return tenantRepository.findByUserId(userId)
                .map(Tenant::getId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Tenant profile not found for current user"
                        )
                );
    }

    @Transactional(readOnly = true)
    public boolean isUserTenant() {

        Integer userId = userService.getCurrentUserId();
        User user = userService.getUser(userId); // fetches user by ID

        return user.getRoles().stream()
                .anyMatch(role -> "TENANT".equals(role.getName()));
    }

    @Transactional
    public boolean submitApplication(Integer listingId) {

        Integer userId = userService.getCurrentUserId();

        Tenant tenant = tenantRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Current user is not a tenant"
                        )
                );

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Listing not found"
                        )
                );

        if (listing.getApplicants().contains(tenant)) {
            return true; // already applied
        }

        if (tenant.getListing() != null) {
            throw new IllegalStateException(
                    "Tenant is already renting a listing"
            );
        }

        tenant.setRentalStatus(RentalStatus.APPLIED);

        listing.addApplicant(tenant);
        tenant.applyToListing(listing);

        /* Ensures persistence */
        tenantRepository.save(tenant);
        listingRepository.save(listing);

        return false;
    }

    @Transactional
    public void approveApplication(Integer tenantId, Integer listingId) {

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Tenant not found"
                        )
                );

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Listing not found"
                        )
                );

        if (listing.getTenant() != null) {
            throw new IllegalStateException(
                    "Listing is already rented"
            );
        }

        if (tenant.getListing() != null) {
            throw new IllegalStateException(
                    "Tenant is already renting another listing"
            );
        }

        tenant.setRentalStatus(RentalStatus.RENTING);
        listing.setTenant(tenant);
        listing.setStatus(ListingStatus.RENTED);
    }

    /* Assigns role 'TENANT' if renting for the first time */
    private void assignTenantRole(User user) {

        Role tenantRole = roleRepository.findByName("TENANT")
                .orElseThrow(() ->
                        new IllegalStateException(
                                "TENANT role not found"
                        )
                );

        if (!user.getRoles().contains(tenantRole)) {
            user.getRoles().add(tenantRole);
            userService.updateUser(user);
        }
    }

    @Transactional
    public void bindTenantToListing(Tenant tenant, Listing listing) {

        if (listing.getTenant() != null) {
            throw new IllegalStateException("Listing already rented.");
        }

        if (tenant.getListing() != null) {
            throw new IllegalStateException("Tenant is already renting another listing.");
        }

        tenant.setRentalStatus(RentalStatus.RENTING);
        listing.setTenant(tenant);
        listing.setStatus(ListingStatus.RENTED);

        listingRepository.save(listing);
        tenantRepository.save(tenant);
    }

    @Transactional
    public void unassignTenantFromListing(Integer listingId, Integer tenantId) {

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found"));
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

        if (!tenant.equals(listing.getTenant())) {
            throw new IllegalStateException("Tenant not assigned to this listing");
        }

        listing.setTenant(null); // unlinks tenant from listing
        listing.makeAvailable();
        tenant.setRentalStatus(RentalStatus.CANCELED);

        tenantRepository.save(tenant);
        listingRepository.save(listing);
    }

    public void validateRentalApplicationRights(User user, Listing listing) {
        if (user == null)
            throw new AccessDeniedException("Unauthenticated");

        if (userService.currentUserHasRole("ADMIN"))
            throw new AccessDeniedException("Admins cannot rent listings");

        if (!listing.isApproved())
            throw new IllegalStateException("Listing not available for rental");

        if (listing.isRented())
            throw new IllegalStateException("Listing already rented");

        if (listing.getOwner() != null &&
                listing.getOwner().getUser().getId().equals(user.getId()))
            throw new AccessDeniedException("Owners cannot rent their own listings");
    }

    @Transactional
    public void prepareTenantForDeletion(Tenant tenant) {

        /* If actively renting, unassign them */
        if (tenant.getListing() != null) {
            unassignTenantFromListing(tenant.getListing().getId(), tenant.getId());
        }

        /* Clear pending applications */
        List<Listing> appliedListings = new ArrayList<>(tenant.getAppliedListings());
        for (Listing listing : appliedListings) {
            listing.getApplicants().remove(tenant);
            listingRepository.save(listing);
        }
        tenant.getAppliedListings().clear();
    }

}
