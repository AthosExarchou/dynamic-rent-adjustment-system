package gr.hua.dit.dras.controllers.tenant;

/* imports */
import gr.hua.dit.dras.dto.TenantCreateRequest;
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.Tenant;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.services.domain.ListingService;
import gr.hua.dit.dras.services.domain.TenantService;
import gr.hua.dit.dras.services.domain.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("tenant")
public class TenantController {

    private final TenantService tenantService;
    private final UserService userService;
    private final ListingService listingService;

    public TenantController(
            TenantService tenantService,
            UserService userService,
            ListingService listingService
    ) {
        this.tenantService = tenantService;
        this.userService = userService;
        this.listingService = listingService;
    }

    /* Apply to rent listing (form) */
    @Secured("USER")
    @GetMapping("/rent/{id}")
    public String showTenantForm(
            @PathVariable("id") Integer listingId,
            Model model
    ) {
        Integer currentUserId = userService.getCurrentUserId();
        User currentUser = userService.getUser(currentUserId);
        Listing listing = listingService.getListing(listingId);

        tenantService.validateRentalApplicationRights(currentUser, listing);

        boolean isAlreadyTenant = tenantService.isUserTenant();

        if (isAlreadyTenant) {
            Tenant tenant = tenantService.getTenant(currentUser.getId());

            if (tenant.getListing() != null) {
                throw new IllegalStateException("You already rent a listing.");
            }
        }

        if (!isAlreadyTenant && !model.containsAttribute("tenant")) {
            model.addAttribute("tenant", new Tenant());
        }

        model.addAttribute("listingId", listingId);
        model.addAttribute("isAlreadyTenant", isAlreadyTenant);

        return "tenant/tenantform";
    }

    /* Tenant applies for a listing */
    @Secured("USER")
    @PostMapping("/rent/{listingId}")
    public String rentListing(
            @PathVariable Integer listingId,
            @Valid @ModelAttribute("tenant") Tenant tenant,
            BindingResult bindingResult,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String phoneNumber,
            RedirectAttributes redirectAttributes
    ) {
        Integer currentUserId = userService.getCurrentUserId();
        User currentUser = userService.getUser(currentUserId);
        Listing listing = listingService.getListing(listingId);

        tenantService.validateRentalApplicationRights(currentUser, listing);

        boolean isAlreadyTenant = tenantService.isUserTenant();

        /* If not a tenant, validates the form fields and creates the profile */
        if (!isAlreadyTenant) {
            if (bindingResult.hasErrors() ||
                    firstName == null || firstName.isBlank() ||
                    lastName == null || lastName.isBlank() ||
                    phoneNumber == null || phoneNumber.isBlank()) {

                redirectAttributes.addFlashAttribute(
                        "org.springframework.validation.BindingResult.tenant", bindingResult);
                redirectAttributes.addFlashAttribute("tenant", tenant);
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Invalid form data. All fields are required.");

                return "redirect:/tenant/rent/" + listingId;
            }

            tenantService.createTenantForCurrentUser(
                    firstName.trim(),
                    lastName.trim(),
                    phoneNumber.trim()
            );
        } else {
            Tenant existingTenant = tenantService.getTenant(currentUser.getId());
            if (existingTenant.getListing() != null) {
                throw new IllegalStateException("You already rent a listing.");
            }
        }

        if (tenantService.submitApplication(listingId)) {
            throw new IllegalStateException("You have already applied for this listing.");
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "Application submitted successfully.");

        return "redirect:/listings";
    }

    /* Admin creates a tenant for a user */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/new")
    public String createTenant(
            @Valid @ModelAttribute("tenantCreateRequest") TenantCreateRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.tenantCreateRequest", bindingResult);
            redirectAttributes.addFlashAttribute("tenantCreateRequest", request);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Please correct the highlighted errors.");

            return "redirect:/tenants/new";
        }

        Tenant tenant = tenantService.createTenantForUser(
                request.getUserId(),
                request.getFirstName(),
                request.getLastName(),
                request.getPhoneNumber()
        );

        if (tenant == null) {
            throw new IllegalStateException("Tenant role revoked or creation failed.");
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "Tenant created successfully.");

        return "redirect:/auth/users";
    }

}
