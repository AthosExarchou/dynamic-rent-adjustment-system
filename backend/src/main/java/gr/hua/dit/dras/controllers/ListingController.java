package gr.hua.dit.dras.controllers;

/* imports */
import gr.hua.dit.dras.entities.*;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.services.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;

@Controller
@RequestMapping("listing")
public class ListingController {

    private final UserService userService;
    private final TenantService tenantService;
    private final ListingService listingService;
    private final OwnerService ownerService;
    private final EmailService emailService;

    public ListingController(
            UserService userService,
            ListingService listingService,
            OwnerService ownerService,
            TenantService tenantService,
            EmailService emailService
    ) {
        this.userService = userService;
        this.listingService = listingService;
        this.ownerService = ownerService;
        this.tenantService = tenantService;
        this.emailService = emailService;
    }

    /* Common model attributes */
    @ModelAttribute
    public void addCommonAttributes(Model model) {
        Integer currentUserId = userService.getCurrentUserId();
        Tenant tenant = null;
        try {
            tenant = tenantService.getTenant(currentUserId);
        } catch (NoSuchElementException e) {
            /* user is not a tenant, ignore */
        }
        model.addAttribute("tenant", tenant);
        model.addAttribute("currentUserId", currentUserId);
    }

    /* Public listings */
    @GetMapping("/listings")
    public String showListings(Model model) {

        model.addAttribute("listings", listingService.getListings());
        return "listing/listings";
    }

    @GetMapping("/listings/local")
    public String showLocalListings(Model model) {

        model.addAttribute("listings", listingService.getLocalListings());
        return "listing/listings";
    }

    @GetMapping("/{id}")
    public String showListing(@PathVariable Integer id, Model model) {

        Listing listing;
        try {
            listing = listingService.getListing(id);
        } catch (ResponseStatusException e) {
            model.addAttribute("errorMessage", "This listing could not be found!");
            return "listing/listings";
        }
        model.addAttribute("listing", listing);
        return "listing/listings";
    }

    /* Owner listings */
    @Secured("OWNER")
    @GetMapping("/mylisting")
    public String myListings(Model model) {

        Integer currentUserId = userService.getCurrentUserId();
        Owner owner = ownerService.getOwner(currentUserId);

        /* fetches listings owned by the current owner */
        List<Listing> ownerListings = listingService.getListingsByOwner(owner);

        model.addAttribute("listings", ownerListings);
        return "listing/mylisting";
    }

    /* Add new listing form */
    @Secured("USER")
    @GetMapping("/new")
    public String addListing(Model model) {

        Listing listing = new Listing();
        model.addAttribute("listing", listing);

        Integer ownerId = ownerService.getOwnerIdForCurrentUser();
        if (ownerId == null) {
            ownerId = userService.getCurrentUserId();
        }
        model.addAttribute("ownerId", ownerId);

        boolean isUserOwner = userService.isUserOwner();
        model.addAttribute("isUserOwner", isUserOwner);

        Integer tenantId = tenantService.getTenantIdForCurrentUser();
        if (tenantId == null) {
            tenantId = userService.getCurrentUserId();
        }
        model.addAttribute("tenantId", tenantId);

        boolean isUserTenant = tenantService.isUserTenant();
        model.addAttribute("isUserTenant", isUserTenant);
        return "listing/listing";
    }

    /* Save new listing */
    @Secured("USER")
    @PostMapping("/new")
    public String saveListing(@Valid @ModelAttribute("listing") Listing listing,
                              BindingResult bindingResult,
                              @RequestParam(value = "owner_id", required = false) Integer ownerId,
                              @RequestParam(value = "firstName", required = false) String firstName,
                              @RequestParam(value = "lastName", required = false) String lastName,
                              @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
                              Model model,
                              HttpSession session
    ) {
        if (bindingResult.hasErrors()) {
            if (ownerId == null && userService.isUserOwner()) {
                ownerId = userService.getCurrentUserId();
            }
            model.addAttribute("ownerId", ownerId);
            model.addAttribute("isUserOwner", userService.isUserOwner());
            return "listing/listing";
        }

        Owner owner;
        /* if the user is not already an owner, creates an owner and assigns the role 'OWNER' */
        if (!userService.isUserOwner()) {
            if (Stream.of(firstName, lastName, phoneNumber)
                    .anyMatch(s -> s == null || s.isBlank())) {

                model.addAttribute("errorMessage",
                        "First name, last name, and phone number are required for new owner.");
                return "listing/listing";
            }
            if (!phoneNumber.matches("^\\+?[0-9. ()-]{7,25}$")) {
                model.addAttribute("errorMessage",
                        "Invalid phone number format. Use 7-25 digits.");
                return "listing/listing";
            }

            owner = ownerService.createOwnerForCurrentUser(
                    firstName.trim(), lastName.trim(), phoneNumber.trim()
            );
            if (owner == null) {
                model.addAttribute("errorMessage",
                        "Your role as 'OWNER' has been revoked by the Administrator." +
                                " Please contact us for further details.");
                return "listing/listing";
            }
            listingService.assignRoleToUserForFirstListing(owner, session);
        } else {
            if (ownerId == null) {
                ownerId = userService.getCurrentUserId();
            }
            owner = ownerService.getOwner(ownerId);
            if (owner == null) {
                model.addAttribute("errorMessage", "Owner not found.");
                return "listing/listing";
            }
        }

        listing.setStatus(ListingStatus.PENDING);
        listing.setTenant(null); //newly added listings start without a tenant
        listing.setExternal(false);
        listingService.saveListing(listing);
        ownerService.assignOwnerToListing(listing.getId(), owner);

        /* Sends email notification to the owner of said listing */
        try {
            emailService.sendEmailNotification(
                    owner.getUser().getEmail(),
                    owner.getFirstName() + " " + owner.getLastName(),
                    listing,
                    "ownerCreated"
            );
        } catch (Exception e) {
            model.addAttribute("emailError",
                    "Listing submitted but confirmation email could not be sent.");
            e.printStackTrace();
        }

        model.addAttribute("listings", listingService.getListings());
        model.addAttribute("successMessage",
                "Your listing was submitted successfully! Awaiting approval.");
        return "listing/listings";
    }

    /* Delete listing */
    @Secured({"OWNER", "ADMIN"})
    @PostMapping("/delete/{id}")
    public String deleteListing(@PathVariable Integer id, Model model) {

        Listing listing;
        try {
            listing = listingService.getListing(id);
        } catch (ResponseStatusException e) {
            model.addAttribute("errorMessage", "Listing not found.");
            return "listing/mylisting";
        }

        /* Cannot delete external listings manually */
        if (listing.isExternal()) {
            model.addAttribute("errorMessage",
                    "External listings cannot be deleted manually.");
            return "listing/mylisting";
        }

        /* Checks if the listing is rented (cannot be deleted) */
        if (listing.getTenant() != null) {
            model.addAttribute("errorMessage",
                    "This listing has an active rental and cannot be deleted at this time.");
            return "listing/mylisting";
        }

        Integer currentUserId = userService.getCurrentUserId();
        /* Checks if the logged-in user is the owner of this listing */
        Owner listingOwner = listing.getOwner();
        if (listingOwner == null || listingOwner.getUser() == null
                || !Objects.equals(listingOwner.getUser().getId(), currentUserId)) {

            model.addAttribute("errorMessage", "You are not authorized to delete this listing!");
            return "listing/mylisting"; //back with error
        }

        String ownerEmail = listingOwner.getUser().getEmail();
        /* Sends email before deleting listing */
        try {
            emailService.sendListingDeletionEmail(ownerEmail, listing);
        } catch (Exception e) {
            model.addAttribute("emailError", "Notification email could not be sent.");
            System.out.println("Error during email sending!");
            e.printStackTrace();
        }
        listingService.validateListingModificationRights(listing, userService.getUser(currentUserId));

        /* Proceeds with the listing deletion */
        System.out.println("Deleting listing with ID: " + id);
        listingService.deleteListing(id);
        System.out.println("Listing deleted successfully.");

        model.addAttribute("listings", listingService.getListings()); //list of remaining listings
        model.addAttribute("successMessage", "Listing deleted successfully!");
        return "listing/mylisting"; //back to the listings list page
    }

    /* Approve listings (admin) */
    @Secured("ADMIN")
    @GetMapping("/listingsforapproval")
    public String showListingsForApproval(Model model) {

        model.addAttribute("listings", listingService.getPendingListings());
        return "listing/listingsforapproval";
    }

    @Secured("ADMIN")
    @PostMapping("/approve/{id}")
    public String approveListing(@PathVariable Integer id, Model model) {

        Listing listing;
        try {
            listing = listingService.getListing(id);
        } catch (ResponseStatusException e) {
            model.addAttribute("errorMessage", "Listing not found.");
            return "listing/listings";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "listing/listings";
        }
        listingService.approveListing(id);

        /* Sends email notification to the owner of said listing */
        try {
            Owner owner = listing.getOwner();
            if (owner != null && owner.getUser() != null) {
                emailService.sendEmailNotification(
                        owner.getUser().getEmail(),
                        owner.getFirstName() + " " + owner.getLastName(),
                        listing,
                        "adminApproved"
                );
            }
        } catch (Exception e) {
            model.addAttribute("emailError",
                    "Listing approved but email could not be sent to the owner.");
        }

        model.addAttribute("successMessage", "Listing approved successfully!");
        return "listing/listings";
    }

    /* Assign tenant/owner */
    @GetMapping("/assign/{id}")
    public String showAssignOwnerToListing(@PathVariable Integer id, Model model) {

        Listing listing = listingService.getListing(id);
        List<Owner> owners = ownerService.getOwners();
        model.addAttribute("listing", listing);
        model.addAttribute("owners", owners);
        return "listing/assignowner";
    }

    @Secured("ADMIN")
    @PostMapping("/assign/{id}")
    public String assignOwnerToListing(
            @PathVariable Integer id,
            @RequestParam(value = "owner_id") Integer ownerId,
            Authentication authentication,
            Model model
    ) {
        User currentUser = userService.getUserByEmail(authentication.getName());
        Listing listing = listingService.getListing(id);
        listingService.validateListingModificationRights(listing, currentUser);

        Owner owner = ownerService.getOwner(ownerId);
        ownerService.assignOwnerToListing(id, owner);

        model.addAttribute("listings", listingService.getListings());
        model.addAttribute("successMessage", "Form submitted successfully!");
        return "listing/listings";
    }

    @Secured("ADMIN")
    @GetMapping("/unassign/owner/{id}")
    public String unassignOwnerFromListing(@PathVariable Integer id, Authentication authentication, Model model) {

        User currentUser = userService.getUserByEmail(authentication.getName());
        Listing listing = listingService.getListing(id);

        listingService.validateListingModificationRights(listing, currentUser);

        ownerService.unassignOwnerFromListing(id);
        model.addAttribute("listings", listingService.getListings());
        return "listing/listings";
    }

    @GetMapping("/tenantassign/{id}")
    public String showAssignTenantToListing(@PathVariable Integer id, Model model) {

        Listing listing = listingService.getListing(id);
        List<Tenant> tenants = tenantService.getTenants();
        model.addAttribute("listing", listing);
        model.addAttribute("tenants", tenants);
        return "listing/assigntenant";
    }

    @Secured({"ADMIN", "OWNER"})
    @PostMapping("/tenantassign/{id}")
    public String assignTenantToListing(
            @PathVariable Integer id,
            @RequestParam(value = "tenant") Integer tenantId,
            Authentication authentication,
            Model model
    ) {
        User currentUser = userService.getUserByEmail(authentication.getName());
        Listing listing = listingService.getListing(id);
        listingService.validateListingModificationRights(listing, currentUser);

        if (listing.getTenant() != null) {
            model.addAttribute("errorMessage", "Listing already has a tenant assigned.");
            return "listing/listings";
        }
        Tenant tenant = tenantService.getTenant(tenantId);
        tenantService.assignTenantToListing(id, tenant, "TENANT");

        model.addAttribute("listings", listingService.getListings());
        return "listing/listings";
    }

    @Secured({"ADMIN", "USER", "OWNER"})
    @GetMapping("/unassign/tenant/{id}")
    public String unassignTenantFromListing(@PathVariable Integer id, Authentication authentication, Model model) {

        User currentUser = userService.getUserByEmail(authentication.getName());
        Listing listing = listingService.getListing(id);
        listingService.validateListingModificationRights(listing, currentUser);

        tenantService.unassignTenantFromListing(id, tenantService.getTenantIdForCurrentUser());
        model.addAttribute("listings", listingService.getListings());
        return "listing/listings";
    }

    @Secured("OWNER")
    @GetMapping("/{id}/applications")
    public String viewApplications(
            @PathVariable("id") Integer listingId,
            Authentication authentication,
            Model model
    ) {
        Listing listing;
        try {
            listing = listingService.getListing(listingId);
        } catch (ResponseStatusException e) {
            model.addAttribute("errorMessage", "Listing not found.");
            return "listing/applications";
        }

        User currentUser = userService.getUserByEmail(authentication.getName());
        listingService.validateListingModificationRights(listing, currentUser);

        if (!listing.getOwner().getUser().getId().equals(userService.getCurrentUserId())) {

            model.addAttribute("errorMessage",
                    "You do not have access to this listing or it does not exist.");
            return "listing/applications";
        }
        model.addAttribute("listing", listing);
        model.addAttribute("applications", listing.getApplicants());
        return "listing/applications";
    }

    /* Every role is allowed to, at the very least, filter listings */
    @GetMapping("/filter")
    public String filterListings(@RequestParam(required = false) String title,
                                   @RequestParam(required = false) Integer minPrice,
                                   @RequestParam(required = false) Integer maxPrice,
                                   Model model) {

        int min = (minPrice != null) ? minPrice : 0;
        int max = (maxPrice != null) ? maxPrice : 20000;

        List<Listing> filteredListings = listingService.filterListings(title, min, max);
        model.addAttribute("listings", filteredListings);
        return "listing/listings";
    }

}
