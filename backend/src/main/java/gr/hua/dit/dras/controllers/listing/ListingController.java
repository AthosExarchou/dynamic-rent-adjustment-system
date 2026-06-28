package gr.hua.dit.dras.controllers.listing;

/* imports */
import gr.hua.dit.dras.dto.ListingFilterDTO;
import gr.hua.dit.dras.dto.ListingCreateDTO;
import gr.hua.dit.dras.entities.*;
import gr.hua.dit.dras.services.domain.*;
import gr.hua.dit.dras.services.application.ListingApplicationService;
import jakarta.servlet.http.HttpSession;
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

    private Map<String, Object> mapListing(Listing l) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", l.getId());
        map.put("title", l.getTitle());
        map.put("subtitle", l.getSubtitle() != null ? l.getSubtitle() : "");
        map.put("description", l.getDescription());
        map.put("price", l.getPrice());
        map.put("pricePerM2", l.getPricePerM2());
        map.put("address", l.getAddress());
        map.put("propertyType", l.getPropertyType() != null ? l.getPropertyType().name() : null);
        map.put("rentalDuration", l.getRentalDuration() != null ? l.getRentalDuration().name() : null);
        map.put("yearBuilt", l.getYearBuilt());
        map.put("sizeM2", l.getSizeM2());
        map.put("status", l.getStatus().name());
        return map;
    }

    /* Public listings */
    @GetMapping({"", "/"})
    public ResponseEntity<?> showListings() {
        List<Map<String, Object>> res = listingService.getListings().stream().map(this::mapListing).collect(Collectors.toList());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/local")
    public ResponseEntity<?> showLocalListings() {
        List<Map<String, Object>> res = listingService.getLocalListings().stream().map(this::mapListing).collect(Collectors.toList());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> showListing(@PathVariable Integer id) {
        Listing l = listingService.getListing(id);
        return ResponseEntity.ok(mapListing(l));
    }

    @GetMapping("/filter")
    public ResponseEntity<?> filterListings(@ModelAttribute ListingFilterDTO filter) {
        List<Map<String, Object>> res = listingService.filterListings(filter).stream().map(this::mapListing).collect(Collectors.toList());
        return ResponseEntity.ok(res);
    }

    /* Owner listings */
    @Secured("OWNER")
    @GetMapping("/mylisting")
    public ResponseEntity<?> myListings() {
        List<Map<String, Object>> res = listingApplicationService.getOwnerListingsForCurrentUser().stream().map(this::mapListing).collect(Collectors.toList());
        return ResponseEntity.ok(res);
    }

    /* Save new listing */
    @PreAuthorize("hasAuthority('USER')")
    @PostMapping("/new")
    public ResponseEntity<?> saveListing(@Valid @ModelAttribute ListingCreateDTO listingDTO,
                              BindingResult bindingResult,
                              @RequestParam(value = "owner_id", required = false) Integer ownerId,
                              @RequestParam(value = "firstName", required = false) String firstName,
                              @RequestParam(value = "lastName", required = false) String lastName,
                              @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
                              HttpSession session
    ) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Validation failed", "details", bindingResult.getAllErrors()));
        }

        Listing listing = new Listing();
        listing.setTitle(listingDTO.getTitle());
        listing.setSubtitle(listingDTO.getSubtitle());
        listing.setDescription(listingDTO.getDescription());
        listing.setPrice(listingDTO.getPrice());
        listing.setPricePerM2(listingDTO.getPricePerM2());
        listing.setAddress(listingDTO.getAddress());
        listing.setPropertyType(listingDTO.getPropertyType());
        listing.setRentalDuration(listingDTO.getRentalDuration());
        listing.setYearBuilt(listingDTO.getYearBuilt());
        listing.setSizeM2(listingDTO.getSizeM2());

        listingApplicationService.createListing(
                listing, ownerId, firstName, lastName, phoneNumber, session
        );
        return ResponseEntity.ok().build();
    }

    /* Delete listing */
    @Secured("OWNER")
    @PostMapping("/delete/{id}")
    public ResponseEntity<?> deleteListing(@PathVariable Integer id) {
        listingApplicationService.deleteListing(id);
        return ResponseEntity.ok().build();
    }

    /* Approve listings (admin) */
    @Secured("ADMIN")
    @GetMapping("/forapproval")
    public ResponseEntity<?> listingsForApproval() {
        List<Map<String, Object>> res = listingService.getPendingListings().stream().map(this::mapListing).collect(Collectors.toList());
        return ResponseEntity.ok(res);
    }

    @Secured("ADMIN")
    @PostMapping("/approve/{id}")
    public ResponseEntity<?> approveListing(@PathVariable Integer id) {
        listingApplicationService.approveListing(id);
        return ResponseEntity.ok().build();
    }

    /* Reject listings (admin) */
    @Secured("ADMIN")
    @PostMapping("/reject/{id}")
    public ResponseEntity<?> rejectListing(@PathVariable Integer id) {
        listingApplicationService.rejectListing(id);
        return ResponseEntity.ok().build();
    }

    /* Assign/Unassign Owner */
    @Secured("ADMIN")
    @PostMapping("/assign/{id}")
    public ResponseEntity<?> assignOwnerToListing(
            @PathVariable Integer id,
            @RequestParam(value = "owner_id") Integer ownerId
    ) {
        listingApplicationService.assignOwner(id, ownerId);
        return ResponseEntity.ok().build();
    }

    @Secured("ADMIN")
    @PostMapping("/unassign/owner/{id}")
    public ResponseEntity<?> unassignOwnerFromListing(@PathVariable Integer id) {
        listingApplicationService.unassignOwner(id);
        return ResponseEntity.ok().build();
    }

    /* Assign/Unassign Tenant */
    @Secured("OWNER")
    @PostMapping("/unassign/tenant/{id}")
    public ResponseEntity<?> unassignTenantFromListing(@PathVariable Integer id) {
        listingApplicationService.unassignTenant(id);
        return ResponseEntity.ok().build();
    }

    /* Applications View */
    @Secured("OWNER")
    @GetMapping("/{id}/applications")
    public ResponseEntity<?> viewApplications(@PathVariable Integer id) {
        Listing listing = listingApplicationService.viewApplications(id);
        
        List<Map<String, Object>> applicants = listing.getApplicants().stream()
            .map(t -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", t.getId());
                map.put("firstName", t.getFirstName());
                map.put("lastName", t.getLastName());
                map.put("phoneNumber", t.getPhoneNumber());
                map.put("status", t.getRentalStatus().name());
                return map;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("listing", mapListing(listing), "applicants", applicants));
    }
}
