package gr.hua.dit.dras.controllers.owner;

/* imports */
import gr.hua.dit.dras.dto.OwnerCreateRequest;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.services.application.ListingApplicationService;
import gr.hua.dit.dras.services.domain.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("owner")
public class OwnerController {

    private final OwnerService ownerService;
    private final UserService userService;
    private final ListingApplicationService listingApplicationService;

    public OwnerController(
            OwnerService ownerService,
            UserService userService,
            ListingApplicationService listingApplicationService
    ) {
        this.ownerService = ownerService;
        this.userService = userService;
        this.listingApplicationService = listingApplicationService;
    }

    @PostMapping("/new")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<?> createOwner(
            @Valid @RequestBody OwnerCreateRequest request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation failed",
                    "details", bindingResult.getAllErrors()
                            .stream()
                            .map(e -> e.getDefaultMessage())
                            .toList()
            ));
        }

        Integer userId;

        // Regular USERs are always forced to use their own ID to prevent IDOR
        boolean isAdmin = userService.currentUserHasRole("ADMIN");
        if (isAdmin && request.getUserId() != null) {
            userId = request.getUserId();
        } else {
            userId = userService.getCurrentUserId();
        }

        ownerService.createOwnerForUser(
                userId,
                request.getFirstName(),
                request.getLastName(),
                request.getPhoneNumber()
        );

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/listings")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> showListings(@PathVariable Integer id) {

        Owner owner = ownerService.getOwner(id);

        Integer currentUserId = userService.getCurrentUserId();
        boolean isAdmin = userService.currentUserHasRole("ADMIN");

        if (!isAdmin && !owner.getUser().getId().equals(currentUserId)) {
            return ResponseEntity.status(403).body(Map.of("error", "You are not authorized to view these listings."));
        }

        /* Protects system owner from direct UI access */
        if (owner.isSystemOwner()) {
            return ResponseEntity.status(403).body(Map.of("error", "System owner listings cannot be viewed."));
        }

        List<Map<String, Object>> visibleListings = owner.getListings()
                .stream()
                .filter(l -> !l.isExternal() && l.getStatus() == ListingStatus.APPROVED)
                .map(l -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", l.getId());
                    map.put("title", l.getTitle());
                    map.put("status", l.getStatus().name());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(visibleListings);
    }

    /* Owner approves tenant's application */
    @Secured("OWNER")
    @PostMapping("/listings/{listingId}/approveApplicant/{tenantId}")
    public ResponseEntity<?> approveTenantApplication(
            @PathVariable Integer listingId,
            @PathVariable Integer tenantId
    ) {
        listingApplicationService.approveTenantApplication(listingId, tenantId);
        return ResponseEntity.ok().build();
    }

    /* Owner rejects tenant's application */
    @Secured("OWNER")
    @PostMapping("/listings/{listingId}/rejectApplicant/{tenantId}")
    public ResponseEntity<?> rejectTenantApplication(
            @PathVariable Integer listingId,
            @PathVariable Integer tenantId
    ) {
        listingApplicationService.rejectTenantApplication(listingId, tenantId);
        return ResponseEntity.ok().build();
    }
}
