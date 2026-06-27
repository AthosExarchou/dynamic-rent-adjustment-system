package gr.hua.dit.dras.controllers.tenant;

import gr.hua.dit.dras.dto.TenantCreateRequest;
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.Tenant;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.services.domain.ListingService;
import gr.hua.dit.dras.services.domain.TenantService;
import gr.hua.dit.dras.services.domain.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
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

    /* Tenant applies for a listing */
    @Secured("USER")
    @PostMapping("/rent/{listingId}")
    public ResponseEntity<?> rentListing(
            @PathVariable Integer listingId,
            @RequestBody(required = false) Map<String, String> payload
    ) {
        Integer currentUserId = userService.getCurrentUserId();
        User currentUser = userService.getUser(currentUserId);
        Listing listing = listingService.getListing(listingId);

        tenantService.validateRentalApplicationRights(currentUser, listing);

        boolean isAlreadyTenant = tenantService.isUserTenant();

        /* Pre-validate existing tenants before any state changes */
        if (isAlreadyTenant) {
            Tenant existingTenant = tenantService.getTenant(currentUser.getId());

            if (existingTenant.getListing() != null) {
                return ResponseEntity.badRequest().body(Map.of("error", "You already rent a listing."));
            }
            if (listing.getApplicants().contains(existingTenant)) {
                return ResponseEntity.badRequest().body(Map.of("error", "You have already applied for this listing."));
            }
        }

        /* If not a tenant, validates the form fields and creates the profile */
        if (!isAlreadyTenant) {
            if (payload == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing profile data."));
            }
            
            String firstName = payload.get("firstName");
            String lastName = payload.get("lastName");
            String phoneNumber = payload.get("phoneNumber");

            if (firstName == null || firstName.isBlank() ||
                lastName == null || lastName.isBlank() ||
                phoneNumber == null || phoneNumber.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid form data. All fields are required."));
            }

            tenantService.createTenantForCurrentUser(
                    firstName.trim(),
                    lastName.trim(),
                    phoneNumber.trim()
            );
        }

        /* All preconditions validated - submit application */
        tenantService.submitApplication(listingId);

        return ResponseEntity.ok().build();
    }

    /* Admin creates a tenant for a user */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/new")
    public ResponseEntity<?> createTenant(
            @Valid @RequestBody TenantCreateRequest request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation failed",
                    "details", bindingResult.getAllErrors()
            ));
        }

        Tenant tenant = tenantService.createTenantForUser(
                request.getUserId(),
                request.getFirstName(),
                request.getLastName(),
                request.getPhoneNumber()
        );

        if (tenant == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tenant role revoked or creation failed."));
        }

        return ResponseEntity.ok().build();
    }
}
