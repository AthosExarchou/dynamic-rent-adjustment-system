package gr.hua.dit.dras.controllers.owner;

/* imports */
import gr.hua.dit.dras.dto.OwnerCreateRequest;
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.entities.Tenant;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.repositories.RoleRepository;
import gr.hua.dit.dras.services.domain.*;
import gr.hua.dit.dras.services.infrastructure.EmailService;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("owner")
public class OwnerController {

    private final OwnerService ownerService;
    private final UserService userService;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final ListingService listingService;
    private final TenantService tenantService;

    public OwnerController(
            OwnerService ownerService,
            UserService userService,
            RoleRepository roleRepository,
            EmailService emailService,
            ListingService listingService,
            TenantService tenantService
    ) {
        this.ownerService = ownerService;
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.emailService = emailService;
        this.listingService = listingService;
        this.tenantService = tenantService;
    }

    @GetMapping("/auth/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String usersPage(Model model) {

        List<User> users = userService.getUsers()
                .stream()
                .filter(u -> !"external-system".equals(u.getUsername()))
                .toList();

        model.addAttribute("users", users);
        model.addAttribute("roles", roleRepository.findAll());

        return "auth/users";
    }

    @PostMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String createOwner(
            @Valid @ModelAttribute("ownerCreateRequest") OwnerCreateRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.ownerCreateRequest", bindingResult);
            redirectAttributes.addFlashAttribute("ownerCreateRequest", request);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Please correct the highlighted errors.");

            return "redirect:/owner/new";
        }

        ownerService.createOwnerForUser(
                request.getUserId(),
                request.getFirstName(),
                request.getLastName(),
                request.getPhoneNumber()
        );

        redirectAttributes.addFlashAttribute("successMessage",
                "Owner created successfully.");

        return "redirect:/auth/users";
    }

    @GetMapping("/{id}/listings")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public String showListings(@PathVariable Integer id, Model model) {

        Owner owner = ownerService.getOwner(id);

        Integer currentUserId = userService.getCurrentUserId();
        boolean isAdmin = userService.currentUserHasRole("ADMIN");

        if (!isAdmin && !owner.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You are not authorized to view these listings.");
        }

        /* Protects system owner from direct UI access */
        if (owner.isSystemOwner()) {
            throw new AccessDeniedException("System owner listings cannot be viewed.");
        }

        /* Filters external listings */
        List<Listing> visibleListings = owner.getListings()
                .stream()
                .filter(l -> !l.isExternal() && l.getStatus() == ListingStatus.APPROVED)
                .collect(Collectors.toList());

        model.addAttribute("listings", visibleListings);
        return "listing/listings";
    }

    @Secured("OWNER")
    @PostMapping("/listings/{listingId}/rejectApplicant/{tenantId}")
    public String rejectTenantApplication(
            @PathVariable Integer listingId,
            @PathVariable Integer tenantId,
            RedirectAttributes redirectAttributes
    ) {
        Integer currentUserId = userService.getCurrentUserId();
        User currentUser = userService.getUser(currentUserId);
        Listing listing = listingService.getListing(listingId);
        Tenant tenant = tenantService.getTenant(tenantId);

        listingService.validateListingModificationRights(listing, currentUser);

        if (!listing.getApplicants().contains(tenant)) {
            throw new IllegalStateException("Tenant did not apply for this listing.");
        }

        listingService.rejectApplicant(listing, tenant);

        try {
            /* Sends email notification to the tenant of said listing */
            if (tenant.getUser() != null) {
                emailService.sendEmailNotification(
                        tenant.getUser().getEmail(),
                        tenant.getFirstName() + " " + tenant.getLastName(),
                        listing,
                        "ownerRejectedApplication"
                );
            }

            redirectAttributes.addFlashAttribute("successMessage",
                    "Tenant application rejected successfully");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("emailError",
                    "Tenant rejected but email could not be sent.");
        }

        return "redirect:/listing/mylisting";
    }

}
