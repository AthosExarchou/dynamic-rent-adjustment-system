package gr.hua.dit.dras.controllers;

/* imports */
import gr.hua.dit.dras.entities.*;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.services.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

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

        Listing listing = listingService.getListing(id);
        if (listing == null) {
            model.addAttribute("errorMessage", "This listing could not be found!");
            return "listing/listings";
        }
        model.addAttribute("listing", listing);
        return "listing/listings";
    }

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

    @Secured("USER")
    @GetMapping("/new")
    public String addListing(Model model) {

        Listing listing = new Listing();
        model.addAttribute("listing", listing);
        Integer ownerId = ownerService.getOwnerIdForCurrentUser();

        if (ownerId == null) {
            ownerId = userService.getCurrentUserId();
        }

        if (ownerId != null) {
            model.addAttribute("ownerId", ownerId);
        }
        boolean isUserOwner = userService.isUserOwner();
        model.addAttribute("isUserOwner", isUserOwner);

        Integer tenantId = tenantService.getTenantIdForCurrentUser();
        if (tenantId == null) {
            tenantId = userService.getCurrentUserId();
        }

        if (tenantId != null) {
            model.addAttribute("tenantId", tenantId);
        }

        boolean isUserTenant = tenantService.isUserTenant();
        model.addAttribute("isUserTenant", isUserTenant);
        return "listing/listing";
    }

    @Secured("USER")
    @PostMapping("/new")
    public String saveListing(@Valid @ModelAttribute("listing") Listing listing,
                                BindingResult theBindingResult,
                                @RequestParam(value = "owner_id", required = false) Integer ownerId,
                                @RequestParam(value = "firstName", required = false) String firstName,
                                @RequestParam(value = "lastName", required = false) String lastName,
                                @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
                                Model model, HttpSession session) {

        if (theBindingResult.hasErrors()) {
            boolean isUserOwner = userService.isUserOwner();

            Integer ownerIdValidation = ownerId;
            if (ownerIdValidation == null && isUserOwner) {
                ownerIdValidation = userService.getCurrentUserId();
            }

            model.addAttribute("isUserOwner", isUserOwner);
            model.addAttribute("ownerId", ownerIdValidation);
            model.addAttribute("listing", listing);
            return "listing/listing";
        }

        Owner owner;
        /* if the user is not already an owner, creates an owner and assigns the role 'OWNER' */
        if (!userService.isUserOwner()) {
            if (firstName == null || lastName == null || phoneNumber == null) {
                model.addAttribute("errorMessage",
                        "First name, last name, and phone number are required for new owner.");
                return "listing/listing";
            }
            if (!phoneNumber.matches("^\\+?[0-9. ()-]{7,25}$")) {
                model.addAttribute("errorMessage", "Invalid phone number format. Use only digits, 7-25 digits.");
                return "listing/listing";
            }

            owner = ownerService.createOwnerForCurrentUser(firstName, lastName, phoneNumber);

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
        listingService.saveListing(listing);
        ownerService.assignOwnerToListing(listing.getId(), owner);

        /* sends email notification to the owner of said listing */
        try {
            emailService.sendEmailNotification(
                    owner.getUser().getEmail(),
                    owner.getFirstName() + " " + owner.getLastName(),
                    listing,
                    "ownerCreated"
            );
        } catch (Exception e) {
            model.addAttribute("emailError", "Listing submitted but confirmation email could not be sent.");
        }

        model.addAttribute("listings", listingService.getListings());
        model.addAttribute("successMessage",
                "Your listing was submitted successfully! Awaiting approval.");
        return "listing/listings";
    }

    @GetMapping("/assign/{id}")
    public String showAssignOwnerToListing(@PathVariable Integer id, Model model) {

        Listing listing = listingService.getListing(id);
        List<Owner> owners = ownerService.getOwners();
        model.addAttribute("listing", listing);
        model.addAttribute("owners", owners);
        return "listing/assignowner";
    }

    @GetMapping("/unassign/owner/{id}")
    public String unassignOwnerToListing(@PathVariable Integer id, Model model) {

        ownerService.unassignOwnerFromListing(id);
        model.addAttribute("listings", listingService.getListings());
        return "listing/listings";
    }

    @GetMapping("/unassign/tenant/{id}")
    public String unassignTenantToListing(@PathVariable Integer id, Model model) {

        tenantService.unassignTenantFromListing(id, tenantService.getTenantIdForCurrentUser());
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

    @PostMapping("/assign/{id}")
    public String assignOwnerToListing(@PathVariable Integer id,
                                       @RequestParam(value = "owner_id", required = true) Integer ownerId,
                                       Model model) {

        System.out.println(ownerId);
        Owner owner = ownerService.getOwner(ownerId);
        Listing listing = listingService.getListing(id);
        System.out.println(listing);
        ownerService.assignOwnerToListing(id, owner);
        model.addAttribute("listings", listingService.getListings());
        model.addAttribute("successMessage", "Form submitted successfully!");
        return "listing/listings";
    }

    @PostMapping("/tenantassign/{id}")
    public String assignTenantToListing(@PathVariable Integer id,
                                        @RequestParam(value = "tenant", required = true) Integer tenantId,
                                        Model model) {

        System.out.println(tenantId);
        Tenant tenant = tenantService.getTenant(tenantId);
        Listing listing = listingService.getListing(id);
        System.out.println(listing);
        String roleUserIs = "tenant";
        tenantService.assignTenantToListing(id, tenant, roleUserIs);
        model.addAttribute("listings", listingService.getListings());
        return "listing/listings";
    }

    @Secured("OWNER")
    @PostMapping("/delete/{id}")
    public String deleteListing(@PathVariable Integer id, Model model) {

        Listing listing = listingService.getListing(id);

        /* checks if the listing is rented (cannot be deleted) */
        if (listing.getTenant() != null) {
            model.addAttribute("errorMessage", "This listing has an active rental and cannot be deleted at this time.");
            return "listing/listings";
        }

        /* if listing is not found */
        if (listing == null) {
            model.addAttribute("errorMessage", "This listing could not be found!");
            return "listing/mylisting"; //back to the listings page
        }

        Integer currentUserId = userService.getCurrentUserId();
        /* check if the logged-in user is the owner of this listing */
        Owner listingOwner = listing.getOwner();
        if (listingOwner == null || listingOwner.getUser() == null || !Objects.equals(listingOwner.getUser().getId(), currentUserId)) {
            model.addAttribute("errorMessage", "You are not authorized to delete this listing!");
            return "listing/mylisting"; //back with error
        }

        /* stores email and any needed data before deletion */
        String ownerEmail = listingOwner.getUser().getEmail();

        /* sends email BEFORE deleting listing */
        try {
            emailService.sendListingDeletionEmail(ownerEmail, listing);
        } catch (Exception e) {
            model.addAttribute("emailError", "Notification email could not be sent.");
            System.out.println("Error during email sending!");
            e.printStackTrace();
        }

        /* proceeds with the listing deletion */
        System.out.println("Deleting listing with ID: " + id);
        listingService.deleteListing(id);
        System.out.println("Listing deleted successfully.");

        model.addAttribute("listings", listingService.getListings()); //list of remaining listings
        model.addAttribute("successMessage", "Listing deleted successfully!");
        return "listing/mylisting"; //back to the listings list page
    }

    @Secured("ADMIN")
    @GetMapping("/listingsforapproval")
    public String showListingsForApproval(Model model) {

        List<Listing> listings = listingService.getListings();

        /* filters listings where approved == false */
        List<Listing> unapprovedListings = listings.stream()
                .filter(listing -> listing.getStatus() == ListingStatus.PENDING)
                .collect(Collectors.toList());

        model.addAttribute("listings", unapprovedListings);
        return "listing/listingsforapproval";
    }

    @Secured("ADMIN")
    @PostMapping("/approve/{id}")
    public String changeApprovedStatus(@PathVariable Integer id, Model model) {

        Listing listing = listingService.getListing(id);

        if (listing == null) {
            model.addAttribute("errorMessage", "This listing could not be found!");
            return "listing/listings";
        }
        listing.setStatus(ListingStatus.APPROVED);
        listingService.saveListing(listing);

        /* sends email notification to the owner of said listing */
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
            model.addAttribute("emailError", "Listing approved but email could not be sent to the owner.");
        }

        model.addAttribute("successMessage", "Listing approved successfully!");
        return "listing/listings";
    }

    @Secured("OWNER")
    @GetMapping("/{id}/applications")
    public String viewApplications(@PathVariable("id") Integer listingId, Model model) {

        Listing listing = listingService.getListing(listingId);

        if (listing == null || !listing.getOwner().getUser().getId().equals(userService.getCurrentUserId())) {
            model.addAttribute("errorMessage", "You do not have access to this listing or it does not exist.");
            return "listing/applications";
        }
        model.addAttribute("listing", listing);
        model.addAttribute("applications", listing.getApplicants());
        return "listing/applications";
    }

    /* every role is allowed to, at the very least, filter listings */
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
