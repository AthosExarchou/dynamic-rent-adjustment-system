package gr.hua.dit.dras.services;

/* imports */
import gr.hua.dit.dras.entities.*;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.model.enums.RentalStatus;
import gr.hua.dit.dras.repositories.TenantRepository;
import gr.hua.dit.dras.repositories.ListingRepository;
import gr.hua.dit.dras.repositories.UserRepository;
import gr.hua.dit.dras.repositories.RoleRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class TenantService {

    private TenantRepository tenantRepository;
    private ListingRepository listingRepository;
    private UserService userService;
    private UserRepository userRepository;
    private RoleRepository roleRepository;

    public TenantService(TenantRepository tenantRepository, ListingRepository listingRepository, UserService userService, UserRepository userRepository, RoleRepository roleRepository) {
        this.tenantRepository = tenantRepository;
        this.listingRepository = listingRepository;
        this.userService = userService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public List<Tenant> getTenants() {
        return tenantRepository.findAll();
    }

    @Transactional
    public void saveTenant(Tenant tenant) {
        tenantRepository.save(tenant);
    }

    @Transactional
    public Tenant getTenant(Integer tenantId) {

        Optional<Tenant> optionalTenant = tenantRepository.findById(tenantId);

        if (optionalTenant.isPresent()) {
            return optionalTenant.get();
        } else {
            Integer currentUserId = userService.getCurrentUserId(); //gets the current user's id
            return tenantRepository.findByUserId(currentUserId)
                    .orElseThrow(() -> new NoSuchElementException("Tenant not found with current user id: " + currentUserId));
        }
    }

    @Transactional
    public Tenant createTenantForCurrentUser(String firstName, String lastName, String phoneNumber) {

        Integer userId = userService.getCurrentUserId();
        User user = userService.getUser(userId); //fetches current user by ID
        /* tenant creation */
        Optional<Tenant> existingTenant = tenantRepository.findByUserId(userId);

        if (existingTenant.isEmpty()) {
            Tenant tenant = new Tenant();
            tenant.setFirstName(firstName);
            tenant.setLastName(lastName);
            tenant.setEmail(user.getEmail());
            tenant.setUsername(user.getUsername());
            tenant.setPhoneNumber(phoneNumber);
            tenant.setUser(user); //associates tenant with the current user
            return tenantRepository.save(tenant);
        } else {
            System.out.println("User with id: " + userId + " is a tenant");
            return null;
        }
    }

    @Transactional
    public void assignRoleToUserForFirstListing(Tenant tenant, HttpSession session) {

        if (isFirstListing(tenant)) {
            User user = userService.getUser(userService.getCurrentUserId());
            Optional<Role> optionalRole = roleRepository.findByName("TENANT");

            if (optionalRole.isPresent()) {
                Role tenantRole = optionalRole.get();
                System.out.println("User roles before adding: " + user.getRoles());
                user.getRoles().add(tenantRole);
                userService.updateUser(user);
                session.invalidate(); //invalidates session to refresh roles
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Role 'TENANT' does not exist in the database");
            }
        }
    }

    @Transactional
    public Integer getTenantIdForCurrentUser() {

        Integer userId = userService.getCurrentUserId();
        System.out.println("Current User id: " + userId);
        Optional<Tenant> tenantOptional = tenantRepository.findByUserId(userId);

        if (tenantOptional.isPresent()) {
            System.out.println("Tenant id: " + tenantOptional.get().getId());
            return tenantOptional.get().getId();
        }
        System.out.println("No tenant found for user id: " + userId);
        return null;
    }

    @Transactional
    public boolean isFirstListing(Tenant tenant) {

        /* checks if the tenant already has a listing */
        return tenant.getListing() == null; //if no listing is linked, it's the first one
    }

    @Transactional
    public boolean isUserTenant() {

        Integer currentUserId = userService.getCurrentUserId();
        if (currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User id must not be null.");
        }
        User currentUser = userService.getUser(currentUserId);

        for (Role role : currentUser.getRoles()) {
            if ("TENANT".equals(role.getName())) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public boolean submitApplication(Integer listingId, Tenant tenant) {

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));

        if (listing.getApplicants().contains(tenant) || tenant.getAppliedListings().contains(listing)) {
            return true;
        }

        tenant.setRentalStatus(RentalStatus.APPLIED);
        listing.addApplicant(tenant);
        tenant.applyToListing(listing);
        tenantRepository.save(tenant);
        listingRepository.save(listing);
        return false;
    }

    @Transactional
    public void approveApplication(Integer tenantId, Integer listingId) {

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found with ID: " + tenantId));
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found with ID: " + listingId));

        /* checks if the listing already has an approved tenant */
        if (listing.getTenant() != null && listing.getTenant().getRentalStatus() == RentalStatus.RENTING) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "This listing is already being rented.");
        }

        if (tenant.getListing() != null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "This tenant is already renting a listing.");
        }

        tenant.setRentalStatus(RentalStatus.RENTING);
        listing.setTenant(tenant);
        tenantRepository.save(tenant);
        listing.setStatus(ListingStatus.RENTED);
        listingRepository.save(listing);
    }

    @Transactional
    public Tenant getTenantByAdmin(@Valid Integer tenantId, String firstName, String lastName, String phoneNumber) {

        User user = userService.getUser(tenantId); //fetches current user by ID
        /* tenant creation */
        Optional<Tenant> existingTenant = tenantRepository.findByUserId(tenantId);

        if (existingTenant.isEmpty()) {
            Tenant tenant = new Tenant();
            tenant.setFirstName(firstName);
            tenant.setLastName(lastName);
            tenant.setEmail(user.getEmail());
            tenant.setUsername(user.getUsername());
            tenant.setPhoneNumber(phoneNumber);
            tenant.setUser(user); //associates tenant with the current user
            tenant.setRentalStatus(RentalStatus.APPLIED);
            user = tenant.getUser();
            user.setTenant(tenant);
            Optional<Role> optionalRole = roleRepository.findByName("TENANT");

            if (optionalRole.isPresent()) {
                Role tenantRole = optionalRole.get();
                System.out.println("User roles before adding: " + user.getRoles());
                if (!user.getRoles().contains(tenantRole)) {
                    user.getRoles().add(tenantRole);
                }
                userService.updateUser(user);
                System.out.println("User roles after adding: " + user.getRoles());
            }
            return tenant;
        }
        return null;
    }

}
