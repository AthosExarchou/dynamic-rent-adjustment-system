package gr.hua.dit.dras.services;

/* imports */
import gr.hua.dit.dras.entities.*;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.model.enums.RentalStatus;
import gr.hua.dit.dras.repositories.TenantRepository;
import gr.hua.dit.dras.repositories.ListingRepository;
import gr.hua.dit.dras.repositories.RoleRepository;
import gr.hua.dit.dras.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@Transactional
public class TenantService {

    private final TenantRepository tenantRepository;
    private final ListingRepository listingRepository;
    private final UserService userService;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ListingService listingService;

    public TenantService(
            TenantRepository tenantRepository,
            ListingRepository listingRepository,
            UserService userService,
            RoleRepository roleRepository,
            UserRepository userRepository,
            ListingService listingService
    ) {
        this.tenantRepository = tenantRepository;
        this.listingRepository = listingRepository;
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.listingService = listingService;
    }

    @Transactional
    public List<Tenant> getTenants() {
        return tenantRepository.findAll();
    }

    @Transactional
    public void saveTenant(Tenant tenant) {
        tenantRepository.save(tenant);
    }

    public Tenant getTenant(Integer tenantId) {

        if (tenantId != null) {
            return tenantRepository.findById(tenantId)
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Tenant not found with id: " + tenantId
                            )
                    );
        }

        Integer currentUserId = userService.getCurrentUserId();

        return tenantRepository.findByUserId(currentUserId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Tenant profile not found for current user"
                        )
                );
    }

    public Tenant createTenantForUser(
            Integer userId,
            String firstName,
            String lastName,
            String phoneNumber
    ) {

        User user = userService.getUser(userId); //fetches user by ID

        tenantRepository.findByUserId(userId).ifPresent(t -> {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User already has a Tenant profile"
            );
        });

        Tenant tenant = new Tenant();
        tenant.setFirstName(firstName);
        tenant.setLastName(lastName);
        tenant.setPhoneNumber(phoneNumber);
        tenant.setRentalStatus(RentalStatus.APPLIED);
        tenant.setUser(user); //associates tenant with the user

        Tenant savedTenant = tenantRepository.save(tenant);

        assignTenantRole(user); //assigns role 'TENANT'

        return savedTenant;
    }

    public void createTenantForCurrentUser(
            String firstName,
            String lastName,
            String phoneNumber
    ) {
        Integer userId = userService.getCurrentUserId();
        createTenantForUser(userId, firstName, lastName, phoneNumber);
    }

    public Integer getTenantIdForCurrentUser() {

        Integer userId = userService.getCurrentUserId();

        return tenantRepository.findByUserId(userId)
                .map(Tenant::getId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Tenant profile not found for current user"
                        )
                );
    }

    public boolean isUserTenant() {

        Integer userId = userService.getCurrentUserId();
        User user = userService.getUser(userId); //fetches user by ID

        return user.getRoles().stream()
                .anyMatch(role -> "TENANT".equals(role.getName()));
    }

    public boolean submitApplication(Integer listingId) {

        Integer userId = userService.getCurrentUserId();

        Tenant tenant = tenantRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Current user is not a tenant"
                        )
                );

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Listing not found"
                        )
                );

        if (listing.getApplicants().contains(tenant)) {
            return true; //already applied
        }

        if (tenant.getListing() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
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

    public void approveApplication(Integer tenantId, Integer listingId) {

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Tenant not found"
                        )
                );

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Listing not found"
                        )
                );

        if (listing.getTenant() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Listing is already rented"
            );
        }

        if (tenant.getListing() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
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
                        new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "TENANT role not found"
                        )
                );

        if (!user.getRoles().contains(tenantRole)) {
            user.getRoles().add(tenantRole);
            userService.updateUser(user);
        }
    }

    @Transactional
    public void assignTenantToListing(Integer listingId, Tenant tenant, String RoleUserIs) {

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));

        listing.setTenant(tenant);
        listing.setStatus(ListingStatus.RENTED);

        Integer currentUserId = userService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Role tenantRole = roleRepository.findByName("TENANT")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        Role ownerRole = roleRepository.findByName("OWNER")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        if (!RoleUserIs.equals("owner")) {
            currentUser.getRoles().add(tenantRole);
            userService.updateUser(currentUser); //saves user
        }
        listingRepository.save(listing);
    }

    @Transactional
    public void unassignTenantFromListing(Integer listingId, Integer tenantId) {

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Listing not found"
                ));
        Tenant tenant = tenantRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Tenant not found"
                ));

        if (!tenant.equals(listing.getTenant())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Tenant not assigned to this listing"
            );
        }

        listing.setTenant(null); //unlinks tenant from listing
        listingService.makeAvailable(listing);

        tenant.setRentalStatus(RentalStatus.CANCELED);

        tenantRepository.save(tenant);
        listingRepository.save(listing);
    }

}
