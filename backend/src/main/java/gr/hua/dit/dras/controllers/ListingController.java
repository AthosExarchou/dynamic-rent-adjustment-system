package gr.hua.dit.dras.controllers;

/* imports */
import gr.hua.dit.dras.dto.ListingFilterDTO;
import gr.hua.dit.dras.entities.*;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.services.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.stream.Stream;

@Controller
@RequestMapping("listings")
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

        User currentUser = userService.getCurrentUserOptional().orElse(null);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentUserId",
                currentUser != null ? currentUser.getId() : null);

        Tenant tenant = null;
        if (currentUser != null) {
            tenant = tenantService.findTenantByUserId(currentUser.getId())
                    .orElse(null);
        }

        model.addAttribute("tenant", tenant);
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

        try {
            Listing listing = listingService.getListing(id);
            model.addAttribute("listing", listing);
            return "listing/listings";
        } catch (ResponseStatusException e) {
            model.addAttribute("errorMessage", "This listing could not be found!");
            return "listing/listings";
        }
    }

    /* Owner listings */
    @Secured("OWNER")
    @GetMapping("/mylisting")
    public String myListings(Model model) {

        User currentUser = userService.getCurrentUserOptional().orElse(null);
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Owner owner = ownerService.getOwner(currentUser.getId());

        /* Fetches listings owned by the current owner */
        List<Listing> ownerListings = listingService.getListingsByOwner(owner);

        model.addAttribute("listings", ownerListings);
        return "listing/mylisting";
    }

    /* Add new listing form */
    @PreAuthorize("hasRole('USER') and !hasRole('ADMIN')")
    @GetMapping("/new")
    public String addListing(Model model) {

        Listing listing = new Listing();
        model.addAttribute("listing", listing);

        User currentUser = userService.getCurrentUserOptional().orElse(null);
        Integer ownerId = ownerService.getOwnerIdForCurrentUser();
        if (ownerId == null && currentUser != null) {
            ownerId = currentUser.getId();
        }
        model.addAttribute("ownerId", ownerId);

        model.addAttribute("isUserOwner", userService.isUserOwner());

        Integer tenantId = tenantService.getTenantIdForCurrentUser();
        if (tenantId == null && currentUser != null) {
            tenantId = currentUser.getId();
        }
        model.addAttribute("tenantId", tenantId);
        model.addAttribute("isUserTenant", tenantService.isUserTenant());

        return "listing/listing";
    }

    /* Save new listing */
    @PreAuthorize("hasRole('USER') and !hasRole('ADMIN')")
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
        User currentUser = userService.getCurrentUserOptional().orElse(null);
        if (currentUser == null) {
            model.addAttribute("errorMessage", "You must be logged in to add a listing.");
            return "listing/listing";
        }

        if (bindingResult.hasErrors()) {
            if (ownerId == null && userService.isUserOwner()) {
                ownerId = userService.getCurrentUserId();
            }
            model.addAttribute("ownerId", ownerId);
            model.addAttribute("isUserOwner", userService.isUserOwner());
            return "listing/listing";
        }

        Owner owner;
        /* If the user is not already an owner, creates an owner and assigns the role 'OWNER' */
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
                ownerId = currentUser.getId();
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
    @Secured("OWNER")
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

        User currentUser = userService.getCurrentUserOptional().orElse(null);
        if (currentUser == null) {
            model.addAttribute("errorMessage",
                    "You are not authorized to delete this listing!");
            return "listing/mylisting";
        }

        /* Checks if the logged-in user is the owner of this listing */
        try {
            listingService.validateListingModificationRights(listing, currentUser);
        } catch (ResponseStatusException e) {
            model.addAttribute("errorMessage",
                    "You are not authorized to delete this listing!");
            return "listing/mylisting";
        }

        String ownerEmail = listing.getOwner().getUser().getEmail();
        /* Sends email before deleting listing */
        try {
            emailService.sendListingDeletionEmail(ownerEmail, listing);
        } catch (Exception e) {
            model.addAttribute("emailError",
                    "Notification email could not be sent.");
        }

        /* Proceeds with the listing deletion */
        listingService.deleteListing(id);

        model.addAttribute("listings", listingService.getListings());
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

        try {
            listingService.approveListing(id);

            /* Sends email notification to the owner of said listing */
            Owner owner = listing.getOwner();
            if (owner != null && owner.getUser() != null) {
                emailService.sendEmailNotification(
                        owner.getUser().getEmail(),
                        owner.getFirstName() + " " + owner.getLastName(),
                        listing,
                        "adminApproved"
                );
            }

            model.addAttribute("successMessage", "Listing approved successfully!");

        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            model.addAttribute("emailError",
                    "Listing approved but email could not be sent to the owner.");
        }

        return "listing/listings";
    }

    /* Reject listings (admin) */
    @Secured("ADMIN")
    @PostMapping("/reject/{id}")
    public String rejectListing(@PathVariable Integer id, Model model) {

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

        try {
            listingService.rejectListing(id);

            /* Sends email notification to the owner of said listing */
            Owner owner = listing.getOwner();
            if (owner != null && owner.getUser() != null) {
                emailService.sendEmailNotification(
                        owner.getUser().getEmail(),
                        owner.getFirstName() + " " + owner.getLastName(),
                        listing,
                        "adminRejected"
                );
            }

            model.addAttribute("successMessage", "Listing rejected successfully!");

        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            model.addAttribute("emailError",
                    "Listing rejected but email could not be sent to the owner.");
        }

        return "listing/listings";
    }

    /* Assign tenant/owner */
    @Secured("ADMIN")
    @GetMapping("/assign/{id}")
    public String showAssignOwnerToListing(@PathVariable Integer id, Model model) {

        Listing listing = listingService.getListing(id);

        model.addAttribute("listing", listing);
        model.addAttribute("owners", ownerService.getOwners());
        return "listing/assignowner";
    }

    @Secured("ADMIN")
    @PostMapping("/assign/{id}")
    public String assignOwnerToListing(
            @PathVariable Integer id,
            @RequestParam(value = "owner_id") Integer ownerId,
            Model model
    ) {
        User currentUser = userService.getCurrentUserOptional().orElse(null);
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Listing listing = listingService.getListing(id);
        try {
            listingService.validateListingModificationRights(listing, currentUser);
        } catch (ResponseStatusException e) {
            model.addAttribute("errorMessage",
                    "You are not authorized to modify this listing!");
            return "listing/listings";
        }

        Owner owner = ownerService.getOwner(ownerId);
        ownerService.assignOwnerToListing(id, owner);

        model.addAttribute("listings", listingService.getListings());
        model.addAttribute("successMessage", "Form submitted successfully!");
        return "listing/listings";
    }

    @Secured("ADMIN")
    @GetMapping("/unassign/owner/{id}")
    public String unassignOwnerFromListing(@PathVariable Integer id, Model model) {

        User currentUser = userService.getCurrentUserOptional().orElse(null);
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Listing listing = listingService.getListing(id);

        try {
            listingService.validateListingModificationRights(listing, currentUser);
        } catch (ResponseStatusException e) {
            model.addAttribute("errorMessage",
                    "You are not authorized to modify this listing!");
            return "listing/listings";
        }

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

    @Secured("OWNER")
    @PostMapping("/tenantassign/{id}")
    public String assignTenantToListing(
            @PathVariable Integer id,
            @RequestParam(value = "tenant") Integer tenantId,
            Model model
    ) {
        User currentUser = userService.getCurrentUserOptional().orElse(null);
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Listing listing = listingService.getListing(id);
        try {
            listingService.validateListingModificationRights(listing, currentUser);
        } catch (ResponseStatusException e) {
            model.addAttribute("errorMessage",
                    "You are not authorized to assign tenants to this listing!");
            return "listing/listings";
        }

        if (listing.getTenant() != null) {
            model.addAttribute("errorMessage",
                    "Listing already has a tenant assigned.");
            return "listing/listings";
        }

        Tenant tenant = tenantService.getTenant(tenantId);
        tenantService.assignTenantToListing(id, tenant, "TENANT");

        model.addAttribute("listings", listingService.getListings());
        return "listing/listings";
    }

    @Secured("OWNER")
    @GetMapping("/unassign/tenant/{id}")
    public String unassignTenantFromListing(@PathVariable Integer id, Model model) {

        User currentUser = userService.getCurrentUserOptional().orElse(null);
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Listing listing = listingService.getListing(id);
        try {
            listingService.validateListingModificationRights(listing, currentUser);
        } catch (ResponseStatusException e) {
            model.addAttribute("errorMessage",
                    "You are not authorized to modify this listing!");
            return "listing/listings";
        }

        tenantService.unassignTenantFromListing(id, tenantService.getTenantIdForCurrentUser());

        model.addAttribute("listings", listingService.getListings());
        return "listing/listings";
    }

    @Secured("OWNER")
    @GetMapping("/{id}/applications")
    public String viewApplications(@PathVariable("id") Integer listingId, Model model) {

        User currentUser = userService.getCurrentUserOptional().orElse(null);
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Listing listing;
        try {
            listing = listingService.getListing(listingId);
        } catch (ResponseStatusException e) {
            model.addAttribute("errorMessage", "Listing not found.");
            return "listing/listings";
        }

        try {
            listingService.validateListingModificationRights(listing, currentUser);
        } catch (ResponseStatusException e) {
            model.addAttribute("errorMessage",
                    "You are not authorized to view applications for this listing!");
            return "listing/listings";
        }

        model.addAttribute("listing", listing);
        model.addAttribute("applications", listing.getApplicants());
        return "listing/applications";
    }

    /* Every role is allowed to, at the very least, filter listings */
    @GetMapping("/filter")
    public String filterListings(ListingFilterDTO filter, Model model) {

        List<Listing> listings = listingService.filterListings(filter);

        model.addAttribute("listings", listings);
        model.addAttribute("filter", filter);
        return "listing/listings";
    }

}
