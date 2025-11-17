package gr.hua.dit.dras.controllers;

/* imports */
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
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("tenant")
public class TenantController {

    private TenantService tenantService;
    private UserService userService;
    private RoleRepository roleRepository;
    private ListingService listingService;
    private EmailService emailService;

    public TenantController(TenantService tenantService, UserService userService, RoleRepository roleRepository, ListingService listingService, EmailService emailService) {
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

            if (tenantService.submitApplication(listingId, tenant)) {
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

    @PostMapping("/new")
    public String saveTenant(@Valid @RequestParam("userId") Integer userId,
                             @RequestParam(value = "firstName", required = false) String firstName,
                             @RequestParam(value = "lastName", required = false) String lastName,
                             @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
                             Model model) {

        if (firstName == null || lastName == null || phoneNumber == null) {
            model.addAttribute("errorMessage",
                    "First name, last name, and phone number are required for new tenant.");
            return "tenant/tenantformforadmin";
        }

        User user = (User) userService.getUser(userId);
        if (user == null) {
            model.addAttribute("errorMessage", "User not found.");
            return "auth/users";
        }

        Tenant tenant = tenantService.getTenantByAdmin(userId, firstName, lastName, phoneNumber);
        if (tenant == null) {
            model.addAttribute("errorMessage",
                    "Your role as 'TENANT' has been revoked by the Administrator." +
                            " Please contact us for further details.");
            return "listing/listing";
        }

        tenantService.saveTenant(tenant);
        model.addAttribute("users", userService.getUsers());
        model.addAttribute("roles", roleRepository.findAll());
        return "auth/users";
    }

    @Secured("USER")
    @PostMapping("/rent/{id}")
    public String rentListing(@PathVariable("id") Integer listingId,
                                @Valid @ModelAttribute("tenant") Tenant tenant,
                                BindingResult theBindingResult,
                                @RequestParam(value = "firstName", required = false) String firstName,
                                @RequestParam(value = "lastName", required = false) String lastName,
                                @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
                                Model model, HttpSession session) {

        if (theBindingResult.hasErrors()) {
            System.out.println(theBindingResult.getAllErrors());
            return "tenant/tenantform";
        }

        Listing listing = listingService.getListing(listingId);

        if (listing == null) {
            model.addAttribute("errorMessage", "This listing could not be found.");
            return "listing/listings";
        }

        if (listing.isRented()) {
            model.addAttribute("errorMessage", "This listing is currently being rented.");
            return "listing/listings";
        }

        Tenant currentTenant;
        /* if the user is not already a tenant, create a new tenant */
        if (!tenantService.isUserTenant()) {
            if (firstName == null || lastName == null || phoneNumber == null) {
                model.addAttribute("errorMessage",
                        "First name, Last name, and phone number are required to rent a listing for the first time.");
                return "tenant/tenantform";
            }
            currentTenant = tenantService.createTenantForCurrentUser(firstName, lastName, phoneNumber);

            if (currentTenant == null) {
                model.addAttribute("errorMessage",
                        "Tenant creation failed. Please contact us for further assistance.");
                return "listing/listings";
            }
            tenantService.assignRoleToUserForFirstListing(tenant, session); //assigns role 'TENANT' if renting for the first time
        } else {
            /* if user is already a tenant, fetch the tenant associated with the current user */
            currentTenant = tenantService.getTenant(userService.getCurrentUserId());

            if (currentTenant == null) {
                model.addAttribute("errorMessage", "Tenant not found.");
                return "listing/listings";
            }
        }

        if (tenantService.submitApplication(listingId, currentTenant)) {
            model.addAttribute("errorMessage",
                    "You have already applied for this listing!");
            return "listing/listings";
        }
        model.addAttribute("successMessage",
                "Application for rental submitted successfully!");
        return "listing/listings";
    }

    @PostMapping("/{listingId}/approveApplication/{tenantId}")
    public String approveApplication(@PathVariable Integer tenantId, @PathVariable Integer listingId, Model model) {

        Listing listing = listingService.getListing(listingId);
        if (listing == null) {
            model.addAttribute("errorMessage", "This listing could not be found.");
            return "listing/mylisting";
        }

        Tenant tenant = tenantService.getTenant(tenantId);
        if (tenant == null) {
            model.addAttribute("errorMessage", "Tenant not found.");
            return "listing/mylisting";
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
        listingService.assignTenantToListing(listingId, tenant, roleUserIs);
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
            model.addAttribute("emailError", "The application was approved, but the confirmation email could not be sent.");
        }

        model.addAttribute("successMessage", "Application approved. This property is now under tenancy.");
        return "listing/mylisting";
    }

}
