package gr.hua.dit.dras.controllers;

/* imports */
import gr.hua.dit.dras.dto.TenantCreateRequest;
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.Tenant;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.services.ListingService;
import gr.hua.dit.dras.services.EmailService;
import gr.hua.dit.dras.services.TenantService;
import gr.hua.dit.dras.services.UserService;
import gr.hua.dit.dras.repositories.RoleRepository;
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

@Controller
@RequestMapping("tenant")
public class TenantController {

    private final TenantService tenantService;
    private final UserService userService;
    private final RoleRepository roleRepository;
    private final ListingService listingService;
    private final EmailService emailService;

    public TenantController(
            TenantService tenantService,
            UserService userService,
            RoleRepository roleRepository,
            ListingService listingService,
            EmailService emailService
    ) {
        this.tenantService = tenantService;
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.listingService = listingService;
        this.emailService = emailService;
    }

    /* Apply to rent listing (form) */
    @Secured("USER")
    @GetMapping("/rent/{id}")
    public String showTenantForm(@PathVariable("id") Integer listingId, Model model) {

        Integer currentUserId = userService.getCurrentUserId();
        User currentUser = userService.getUser(currentUserId);
        Listing listing = listingService.getListing(listingId);

        try {
            tenantService.validateRentalApplicationRights(currentUser, listing);
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "listing/listings";
        }

        if (tenantService.isUserTenant()) {
            Tenant tenant = tenantService.getTenant(currentUser.getId());

            if (tenant.getListing() != null) {
                model.addAttribute("errorMessage",
                        "You already rent a listing.");
                return "listing/listings";
            }

            if (tenantService.submitApplication(listingId)) {
                model.addAttribute("errorMessage",
                        "You have already applied for this listing.");
                return "listing/listings";
            }

            model.addAttribute("successMessage",
                    "Application submitted successfully.");
            return "listing/listings";
        }

        model.addAttribute("tenant", new Tenant());
        model.addAttribute("listingId", listingId);
        return "tenant/tenantform";
    }

    /* Tenant applies for a listing */
    @Secured("USER")
    @PostMapping("/rent/{listingId}")
    public String rentListing(
            @PathVariable Integer listingId,
            @Valid @ModelAttribute("tenant") Tenant tenant,
            BindingResult bindingResult,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String phoneNumber,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", "Invalid form data.");
            return "tenant/tenantform";
        }

        Integer currentUserId = userService.getCurrentUserId();
        User currentUser = userService.getUser(currentUserId);

        Listing listing = listingService.getListing(listingId);

        try {
            tenantService.validateRentalApplicationRights(currentUser, listing);
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "listing/listings";
        }

        if (!tenantService.isUserTenant()) {
            tenantService.createTenantForCurrentUser(
                    firstName.trim(),
                    lastName.trim(),
                    phoneNumber.trim()
            );
        }

        if (tenantService.submitApplication(listingId)) {
            model.addAttribute("errorMessage",
                    "You have already applied for this listing.");
            return "listing/listings";
        }

        model.addAttribute("successMessage",
                "Application submitted successfully.");
        return "listing/listings";
    }

    /* Admin creates a tenant for a user */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/new")
    public String createTenant(
            @Valid @ModelAttribute TenantCreateRequest request,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "tenant/tenantformforadmin";
        }

        Tenant tenant = tenantService.createTenantForUser(
                request.getUserId(),
                request.getFirstName(),
                request.getLastName(),
                request.getPhoneNumber()
        );

        if (tenant == null) {
            model.addAttribute("errorMessage", "Tenant role revoked or creation failed.");
            return "listing/listing";
        }

        model.addAttribute("users", userService.getUsers()
                .stream().filter(u -> !"external-system".equals(u.getUsername())).toList());
        model.addAttribute("roles", roleRepository.findAll());
        return "auth/users";
    }

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/{listingId}/approveApplication/{tenantId}")
    public String approveApplication(
            @PathVariable Integer tenantId,
            @PathVariable Integer listingId,
            Model model
    ) {
        Integer currentUserId = userService.getCurrentUserId();
        User currentUser = userService.getUser(currentUserId);

        Listing listing = listingService.getListing(listingId);
        Tenant tenant = tenantService.getTenant(tenantId);

        /* Only owner of this listing or admin can approve */
        try {
            listingService.validateListingModificationRights(listing, currentUser);
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "listing/mylisting";
        }

        /* Checks that the tenant is not already renting another listing */
        if (tenant.getListing() != null) {
            model.addAttribute("errorMessage",
                    "Tenant already rents a listing.");
            return "listing/mylisting";
        }

        /* Checks listing availability */
        if (listing.isRented()) {
            model.addAttribute("errorMessage", "Listing already rented.");
            return "listing/mylisting";
        }

        /* Assigns tenant and approves application */
        tenantService.assignTenantToListing(listingId, tenant, "OWNER");
        tenantService.approveApplication(tenantId, listingId);

        try {
            emailService.sendEmailNotification(
                    tenant.getUser().getEmail(),
                    tenant.getFirstName() + " " + tenant.getLastName(),
                    listing,
                    "tenantApproval"
            );
        } catch (Exception ignored) {
            model.addAttribute("emailError",
                    "Approved but email could not be sent.");
        }

        model.addAttribute("successMessage", "Application approved.");
        return "listing/mylisting";
    }

}
