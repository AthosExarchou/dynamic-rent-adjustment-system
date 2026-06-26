package gr.hua.dit.dras.controllers.listing;

/* imports */
import gr.hua.dit.dras.dto.ListingFilterDTO;
import gr.hua.dit.dras.entities.*;
import gr.hua.dit.dras.services.domain.*;
import gr.hua.dit.dras.services.application.ListingApplicationService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    @GetMapping({"", "/"})
    public String showListings(Model model) {

        model.addAttribute("listings", listingService.getListings());
        return "listing/listings";
    }

    @GetMapping("/local")
    public String showLocalListings(Model model) {

        model.addAttribute("listings", listingService.getLocalListings());
        return "listing/listings";
    }

    @GetMapping("/{id}")
    public String showListing(@PathVariable Integer id, Model model) {

        model.addAttribute("listing", listingService.getListing(id));
        return "listing/listings";
    }

    @GetMapping("/filter")
    public String filterListings(ListingFilterDTO filter, Model model) {

        model.addAttribute("listings", listingService.filterListings(filter));
        model.addAttribute("filter", filter);
        return "listing/listings";
    }

    /* Owner listings */
    @Secured("OWNER")
    @GetMapping("/mylisting")
    public String myListings(Model model) {

        model.addAttribute("listings", listingApplicationService.getOwnerListingsForCurrentUser());
        return "listing/mylisting";
    }

    /* Add new listing form */
    @PreAuthorize("hasAuthority('USER') and !hasAuthority('ADMIN')")
    @GetMapping("/new")
    public String addListing(Model model) {

        if (!model.containsAttribute("listing")) {
            model.addAttribute("listing", new Listing());
        }

        User currentUser = userService.getCurrentUserOptional().orElse(null);
        Integer ownerId = ownerService.getOwnerIdForCurrentUser();
        if (ownerId == null && currentUser != null) {
            ownerId = currentUser.getId();
        }

        model.addAttribute("ownerId", ownerId);
        model.addAttribute("isUserOwner", userService.isUserOwner());
        model.addAttribute("tenantId", tenantService.getTenantIdForCurrentUser());
        model.addAttribute("isUserTenant", tenantService.isUserTenant());

        return "listing/listing";
    }

    /* Save new listing */
    @PreAuthorize("hasAuthority('USER') and !hasAuthority('ADMIN')")
    @PostMapping("/new")
    public String saveListing(@Valid @ModelAttribute("listing") Listing listing,
                              BindingResult bindingResult,
                              @RequestParam(value = "owner_id", required = false) Integer ownerId,
                              @RequestParam(value = "firstName", required = false) String firstName,
                              @RequestParam(value = "lastName", required = false) String lastName,
                              @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
                              RedirectAttributes redirectAttributes,
                              HttpSession session
    ) {
        /* Validation Check, redirects back to form with data */
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.listing", bindingResult);
            redirectAttributes.addFlashAttribute("listing", listing);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Please correct the highlighted errors.");

            return "redirect:/listings/new";
        }

        listingApplicationService.createListing(
                listing, ownerId, firstName, lastName, phoneNumber, session
        );

        redirectAttributes.addFlashAttribute("successMessage",
                "Your listing was submitted successfully! Awaiting approval.");
        return "redirect:/listings";
    }

    /* Delete listing */
    @Secured("OWNER")
    @PostMapping("/delete/{id}")
    public String deleteListing(@PathVariable Integer id, RedirectAttributes redirectAttributes) {

        listingApplicationService.deleteListing(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "Listing deleted successfully!");
        return "redirect:/listings/mylisting";
    }

    /* Approve listings (admin) */
    @Secured("ADMIN")
    @GetMapping("/forapproval")
    public String listingsForApproval(Model model) {

        model.addAttribute("listings", listingService.getPendingListings());
        return "listing/listingsforapproval";
    }

    @Secured("ADMIN")
    @PostMapping("/approve/{id}")
    public String approveListing(@PathVariable Integer id, RedirectAttributes redirectAttributes) {

        listingApplicationService.approveListing(id);

        redirectAttributes.addFlashAttribute("successMessage",
                "Listing approved successfully!");
        return "redirect:/listings";
    }

    /* Reject listings (admin) */
    @Secured("ADMIN")
    @PostMapping("/reject/{id}")
    public String rejectListing(@PathVariable Integer id, RedirectAttributes redirectAttributes) {

        listingApplicationService.rejectListing(id);

        redirectAttributes.addFlashAttribute("successMessage",
                "Listing rejected successfully!");
        return "redirect:/listings";
    }

    /* Assign tenant/owner */
    @Secured("ADMIN")
    @GetMapping("/assign/{id}")
    public String showAssignOwnerToListing(@PathVariable Integer id, Model model) {

        model.addAttribute("listing", listingService.getListing(id));
        model.addAttribute("owners", ownerService.getOwners());
        return "listing/assignowner";
    }

    @Secured("ADMIN")
    @PostMapping("/assign/{id}")
    public String assignOwnerToListing(
            @PathVariable Integer id,
            @RequestParam(value = "owner_id") Integer ownerId,
            RedirectAttributes redirectAttributes
    ) {
        listingApplicationService.assignOwner(id, ownerId);

        redirectAttributes.addFlashAttribute("successMessage",
                "Owner assigned successfully!");
        return "redirect:/listings";
    }

    @Secured("ADMIN")
    @GetMapping("/unassign/owner/{id}")
    public String unassignOwnerFromListing(@PathVariable Integer id, RedirectAttributes redirectAttributes) {

        listingApplicationService.unassignOwner(id);

        redirectAttributes.addFlashAttribute("successMessage",
                "Owner unassigned successfully!");
        return "redirect:/listings";
    }

    @Secured("USER")
    @GetMapping("/tenantassign/{id}")
    public String showAssignTenantToListing(@PathVariable Integer id, Model model) {

        model.addAttribute("listing", listingService.getListing(id));
        model.addAttribute("tenants", tenantService.getTenants());
        return "listing/assigntenant";
    }

    @Secured("OWNER")
    @PostMapping("/unassign/tenant/{id}")
    public String unassignTenantFromListing(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes
    ) {
        listingApplicationService.unassignTenant(id);

        redirectAttributes.addFlashAttribute("successMessage",
                "Tenant unassigned successfully!");

        return "redirect:/listings/mylisting";
    }

    /* Applications View */
    @Secured("OWNER")
    @GetMapping("/{id}/applications")
    public String viewApplications(@PathVariable Integer id, Model model) {

        Listing listing = listingApplicationService.viewApplications(id);

        model.addAttribute("listing", listing);
        model.addAttribute("applications", listing.getApplicants());
        return "listing/applications";
    }

}
