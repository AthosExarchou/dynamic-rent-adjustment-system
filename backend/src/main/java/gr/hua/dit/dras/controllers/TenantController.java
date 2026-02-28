package gr.hua.dit.dras.controllers;

/* imports */
import gr.hua.dit.dras.dto.TenantCreateRequest;
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.Tenant;
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

    @Secured("USER")
    @GetMapping("/rent/{id}")
    public String showTenantForm(@PathVariable("id") Integer listingId, Model model) {

        model.addAttribute("listingId", listingId);
        Tenant tenant;
        if (tenantService.isUserTenant()) {
            tenant = tenantService.getTenant(userService.getCurrentUserId());
            if (tenant == null) {
                model.addAttribute("errorMessage", "Tenant not found.");
                return "listing/listings";
            }

            if (tenant.getListing() != null) {
                model.addAttribute("errorMessage",
                        "You are already renting a listing. You can't rent or apply for another listing!");
                return "listing/listings";
            }

            if (tenantService.submitApplication(listingId)) {
                model.addAttribute("errorMessage",
                        "You have already applied for this listing!");
                return "listing/listings";
            }
            model.addAttribute("successMessage",
                    "Application for rental submitted successfully!");
            return "listing/listings";
        }
        tenant = new Tenant();
        model.addAttribute("tenant", tenant);
        return "tenant/tenantform";
    }

    /* Tenant applies for a listing */
    @Secured("USER")
    @PostMapping("/rent/{listingId}")
    public String rentListing(
            @PathVariable Integer listingId,
            @Valid @ModelAttribute("tenant") Tenant tenant,
            BindingResult bindingResult,
            @RequestParam(value = "firstName", required = false) String firstName,
            @RequestParam(value = "lastName", required = false) String lastName,
            @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            Model model,
            HttpSession session
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", "Form validation failed.");
            return "tenant/tenantform";
        }

        Listing listing = listingService.getListing(listingId);

        if (listing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found");
        }

        if (listing.isRented()) {
            model.addAttribute("errorMessage",
                    "This listing is currently rented.");
            return "listing/listings";
        }

        /* If user is not yet tenant, create profile */
        if (!tenantService.isUserTenant()) {

            if (firstName == null || lastName == null || phoneNumber == null) {
                model.addAttribute("errorMessage", "All fields are required.");
                return "tenant/tenantform";
            }

            tenantService.createTenantForCurrentUser(firstName, lastName, phoneNumber);
        }

        /* Submit application */
        if (tenantService.submitApplication(listingId)) {
            model.addAttribute("errorMessage",
                    "You have already applied for this listing!");
            return "listing/listings";
        }
        model.addAttribute("successMessage",
                "Application submitted successfully!");
        return "listing/listings";
    }

    /* Admin creates a tenant for a user */
    @PostMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
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

    @PostMapping("/{listingId}/approveApplication/{tenantId}")
    @PreAuthorize("hasRole('OWNER')")
    public String approveApplication(
            @PathVariable Integer tenantId,
            @PathVariable Integer listingId,
            Model model
    ) {
        Listing listing = listingService.getListing(listingId);
        if (listing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found");
        }

        Tenant tenant = tenantService.getTenant(tenantId);
        if (tenant == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
        }

        if (tenant.getListing() != null) {
            model.addAttribute("errorMessage", "This tenant is already renting a listing.");
            return "listing/mylisting";
        }

        if (listing.isRented()) {
            model.addAttribute("errorMessage", "This listing is being rented.");
            return "listing/mylisting";
        }

        String roleUserIs = "owner";
        tenantService.assignTenantToListing(listingId, tenant, roleUserIs);
        tenantService.approveApplication(tenantId, listingId);

        /* sends email notification to the specified applicant of said listing */
        try {
            emailService.sendEmailNotification(
                    tenant.getUser().getEmail(),
                    tenant.getFirstName() + " " + tenant.getLastName(),
                    listing,
                    "tenantApproval"
            );
        } catch (Exception e) {
            model.addAttribute("emailError",
                    "Approval succeeded but email could not be sent.");
        }

        model.addAttribute("successMessage", "Application approved.");
        return "listing/mylisting";
    }

}
