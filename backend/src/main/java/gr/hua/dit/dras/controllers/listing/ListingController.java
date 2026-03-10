package gr.hua.dit.dras.controllers.listing;

/* imports */
import gr.hua.dit.dras.dto.ListingFilterDTO;
import gr.hua.dit.dras.entities.*;
import gr.hua.dit.dras.services.domain.*;
import gr.hua.dit.dras.services.application.ListingApplicationService;
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

@Controller
@RequestMapping("listings")
public class ListingController {

    private final UserService userService;
    private final TenantService tenantService;
    private final ListingService listingService;
    private final OwnerService ownerService;
    private final ListingApplicationService listingApplicationService;

    public ListingController(
            UserService userService,
            ListingService listingService,
            OwnerService ownerService,
            TenantService tenantService,
            ListingApplicationService listingApplicationService
    ) {
        this.userService = userService;
        this.listingService = listingService;
        this.ownerService = ownerService;
        this.tenantService = tenantService;
        this.listingApplicationService = listingApplicationService;
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

        model.addAttribute("listing", new Listing());

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

        try {
            listingApplicationService.createListing(
                    listing, ownerId, firstName, lastName, phoneNumber, session
            );
            model.addAttribute("successMessage",
                    "Your listing was submitted successfully! Awaiting approval.");

        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "listing/listing";
        } catch (RuntimeException e) {
            model.addAttribute("emailError", e.getMessage());
        }

        model.addAttribute("listings", listingService.getListings());
        return "listing/listings";
    }

    /* Delete listing */
    @Secured("OWNER")
    @PostMapping("/delete/{id}")
    public String deleteListing(@PathVariable Integer id, Model model) {

        try {
            listingApplicationService.deleteListing(id);
            model.addAttribute("successMessage", "Listing deleted successfully!");
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
        } catch (ResponseStatusException e) {
            model.addAttribute("errorMessage", "Listing not found or access denied.");
        } catch (RuntimeException e) {
            model.addAttribute("emailError", e.getMessage());
        }

        model.addAttribute("listings", listingService.getListings());
        return "listing/mylisting";
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

        try {
            listingApplicationService.approveListing(id);
            model.addAttribute("successMessage", "Listing approved successfully!");
        } catch (ResponseStatusException | IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
        } catch (RuntimeException e) {
            model.addAttribute("emailError", e.getMessage());
        }
        return "listing/listings";
    }

    /* Reject listings (admin) */
    @Secured("ADMIN")
    @PostMapping("/reject/{id}")
    public String rejectListing(@PathVariable Integer id, Model model) {

        try {
            listingApplicationService.rejectListing(id);
            model.addAttribute("successMessage", "Listing rejected successfully!");
        } catch (ResponseStatusException | IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
        } catch (RuntimeException e) {
            model.addAttribute("emailError", e.getMessage());
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
        try {
            listingApplicationService.assignOwner(id, ownerId);
            model.addAttribute("successMessage", "Owner assigned successfully!");
        } catch (IllegalStateException | ResponseStatusException e) {
            model.addAttribute("errorMessage", e.getMessage());
        }

        model.addAttribute("listings", listingService.getListings());
        model.addAttribute("successMessage", "Form submitted successfully!");
        return "listing/listings";
    }

    @Secured("ADMIN")
    @GetMapping("/unassign/owner/{id}")
    public String unassignOwnerFromListing(@PathVariable Integer id, Model model) {

        try {
            listingApplicationService.unassignOwner(id);
            model.addAttribute("successMessage", "Owner unassigned successfully!");
        } catch (IllegalStateException | ResponseStatusException e) {
            model.addAttribute("errorMessage", e.getMessage());
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
        try {
            listingApplicationService.assignTenant(id, tenantId);
            model.addAttribute("successMessage", "Tenant assigned successfully!");
        } catch (IllegalStateException | ResponseStatusException e) {
            model.addAttribute("errorMessage", e.getMessage());
        }

        model.addAttribute("listings", listingService.getListings());
        return "listing/listings";
    }

    @Secured("OWNER")
    @GetMapping("/unassign/tenant/{id}")
    public String unassignTenantFromListing(@PathVariable Integer id, Model model) {

        try {
            listingApplicationService.unassignTenant(id);
            model.addAttribute("successMessage", "Tenant unassigned successfully!");
        } catch (IllegalStateException | ResponseStatusException e) {
            model.addAttribute("errorMessage", e.getMessage());
        }

        model.addAttribute("listings", listingService.getListings());
        return "listing/listings";
    }

    @Secured("OWNER")
    @GetMapping("/{id}/applications")
    public String viewApplications(@PathVariable Integer id, Model model) {

        try {
            Listing listing = listingApplicationService.viewApplications(id);
            model.addAttribute("listing", listing);
            model.addAttribute("applications", listing.getApplicants());
            return "listing/applications";
        } catch (IllegalStateException | ResponseStatusException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("listings", listingService.getListings());
            return "listing/listings";
        }
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
