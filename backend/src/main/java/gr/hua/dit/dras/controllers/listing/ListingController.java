package gr.hua.dit.dras.controllers.listing;

/* imports */
import gr.hua.dit.dras.dto.ListingFilterDTO;
import gr.hua.dit.dras.dto.ListingCreateDTO;
import gr.hua.dit.dras.dto.PriceSuggestionDTO;
import gr.hua.dit.dras.dto.PriceSuggestionRequestDTO;
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
    private final PriceSuggestionService priceSuggestionService;
    private final ImageStorageService imageStorageService;

    public ListingController(
            UserService userService,
            ListingService listingService,
            OwnerService ownerService,
            TenantService tenantService,
            ListingApplicationService listingApplicationService,
            PriceSuggestionService priceSuggestionService,
            ImageStorageService imageStorageService
    ) {
        this.userService = userService;
        this.listingService = listingService;
        this.ownerService = ownerService;
        this.tenantService = tenantService;
        this.listingApplicationService = listingApplicationService;
        this.priceSuggestionService = priceSuggestionService;
        this.imageStorageService = imageStorageService;
    }

    public static Map<String, Object> mapListing(Listing l) {
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
        map.put("floor", l.getFloor());
        map.put("bedrooms", l.getBedrooms());
        map.put("bathrooms", l.getBathrooms());
        map.put("images", l.getImages());
        map.put("status", l.getStatus().name());
        map.put("rented", l.isRented());
        map.put("rejectionReason", l.getRejectionReason());
        if (l.getOwner() != null) {
            Map<String, Object> ownerMap = new java.util.HashMap<>();
            ownerMap.put("id", l.getOwner().getId());
            if (l.getOwner().getUser() != null) {
                ownerMap.put("userId", l.getOwner().getUser().getId());
            }
            map.put("owner", ownerMap);
        }
        return map;
    }

    /* Public listings */
    @GetMapping({"", "/"})
    public ResponseEntity<?> showListings() {
        List<Map<String, Object>> res = listingService.getListings().stream().map(ListingController::mapListing).collect(Collectors.toList());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/local")
    public ResponseEntity<?> showLocalListings() {
        List<Map<String, Object>> res = listingService.getLocalListings().stream().map(ListingController::mapListing).collect(Collectors.toList());
        return ResponseEntity.ok(res);
    }

    /* Price suggestion based on ARIMA forecast and scraped real estate benchmarks */
    @GetMapping("/price-suggestion")
    public ResponseEntity<?> getPriceSuggestion(@Valid @ModelAttribute PriceSuggestionRequestDTO request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation failed",
                    "details", bindingResult.getAllErrors().stream().map(e -> e.getDefaultMessage()).toList()
            ));
        }
        PriceSuggestionDTO suggestion = priceSuggestionService.suggestPrice(request);
        return ResponseEntity.ok(suggestion);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> showListing(@PathVariable Integer id) {
        Listing l = listingService.getListing(id);
        return ResponseEntity.ok(mapListing(l));
    }

    @GetMapping("/filter")
    public ResponseEntity<?> filterListings(@Valid @ModelAttribute ListingFilterDTO filter) {
        List<Map<String, Object>> res = listingService.filterListings(filter).stream().map(ListingController::mapListing).collect(Collectors.toList());
        return ResponseEntity.ok(res);
    }

    /* Owner listings */
    @Secured("OWNER")
    @GetMapping("/mylisting")
    public ResponseEntity<?> myListings() {
        List<Map<String, Object>> res = listingApplicationService.getOwnerListingsForCurrentUser().stream().map(ListingController::mapListing).collect(Collectors.toList());
        return ResponseEntity.ok(res);
    }

    /* Save new listing */
    @PreAuthorize("hasAuthority('USER') and !hasAuthority('ADMIN')")
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
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation failed",
                    "details", bindingResult.getAllErrors().stream().map(e -> e.getDefaultMessage()).toList()
            ));
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
        listing.setFloor(listingDTO.getFloor());
        listing.setBedrooms(listingDTO.getBedrooms());
        listing.setBathrooms(listingDTO.getBathrooms());

        List<String> combinedImages = new java.util.ArrayList<>();
        if (listingDTO.getImages() != null) {
            combinedImages.addAll(listingDTO.getImages().stream().filter(s -> s != null && !s.isBlank()).toList());
        }
        if (listingDTO.getImageFiles() != null && !listingDTO.getImageFiles().isEmpty()) {
            combinedImages.addAll(imageStorageService.storeImages(listingDTO.getImageFiles()));
        }
        if (combinedImages.size() > 10) {
            combinedImages = combinedImages.subList(0, 10);
        }
        listing.setImages(combinedImages);
        listingDTO.setImages(combinedImages);

        listingApplicationService.createListing(
                listing, ownerId, firstName, lastName, phoneNumber, session
        );
        return ResponseEntity.ok().build();
    }

    /* Edit listing */
    @PreAuthorize("hasAnyAuthority('OWNER', 'ADMIN')")
    @PostMapping("/edit/{id}")
    public ResponseEntity<?> editListing(
            @PathVariable Integer id,
            @Valid @ModelAttribute ListingCreateDTO listingDTO,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation failed",
                    "details", bindingResult.getAllErrors().stream().map(e -> e.getDefaultMessage()).toList()
            ));
        }

        List<String> combinedImages = new java.util.ArrayList<>();
        if (listingDTO.getImages() != null) {
            combinedImages.addAll(listingDTO.getImages().stream().filter(s -> s != null && !s.isBlank()).toList());
        }
        if (listingDTO.getImageFiles() != null && !listingDTO.getImageFiles().isEmpty()) {
            combinedImages.addAll(imageStorageService.storeImages(listingDTO.getImageFiles()));
        }
        if (combinedImages.size() > 10) {
            combinedImages = combinedImages.subList(0, 10);
        }
        listingDTO.setImages(combinedImages);

        Listing updated = listingApplicationService.editListing(id, listingDTO);
        return ResponseEntity.ok(mapListing(updated));
    }

    /* Delete listing */
    @Secured("OWNER")
    @PostMapping("/delete/{id}")
    public ResponseEntity<?> deleteListing(@PathVariable Integer id) {
        listingApplicationService.deleteListing(id);
        return ResponseEntity.ok().build();
    }

    /* Disable / Enable listing */
    @PreAuthorize("hasAnyAuthority('OWNER', 'ADMIN')")
    @PostMapping("/disable/{id}")
    public ResponseEntity<?> disableListing(@PathVariable Integer id) {
        listingApplicationService.disableListing(id);
        return ResponseEntity.ok(Map.of("message", "Listing disabled successfully"));
    }

    @PreAuthorize("hasAnyAuthority('OWNER', 'ADMIN')")
    @PostMapping("/enable/{id}")
    public ResponseEntity<?> enableListing(@PathVariable Integer id) {
        listingApplicationService.enableListing(id);
        return ResponseEntity.ok(Map.of("message", "Listing enabled successfully"));
    }

    @PreAuthorize("hasAnyAuthority('OWNER', 'ADMIN')")
    @PostMapping("/toggle-status/{id}")
    public ResponseEntity<?> toggleListingStatus(@PathVariable Integer id) {
        listingApplicationService.toggleListingStatus(id);
        return ResponseEntity.ok(Map.of("message", "Listing status toggled successfully"));
    }

    /* Approve listings (admin) */
    @Secured("ADMIN")
    @GetMapping("/forapproval")
    public ResponseEntity<?> listingsForApproval() {
        List<Map<String, Object>> res = listingService.getPendingListings().stream().map(ListingController::mapListing).collect(Collectors.toList());
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
    public ResponseEntity<?> rejectListing(
            @PathVariable Integer id,
            @RequestParam(value = "reason", required = false) String reasonParam,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String reason = null;
        if (reasonParam != null && !reasonParam.trim().isBlank()) {
            reason = reasonParam.trim();
        } else if (body != null && body.containsKey("reason") && body.get("reason") != null && !body.get("reason").trim().isBlank()) {
            reason = body.get("reason").trim();
        }

        if (reason == null || reason.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Rejection reason is required"));
        }
        if (reason.length() > 500) {
            return ResponseEntity.badRequest().body(Map.of("error", "Rejection reason cannot exceed 500 characters"));
        }

        listingApplicationService.rejectListing(id, reason);
        return ResponseEntity.ok(Map.of("message", "Listing rejected successfully"));
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
                map.put("email", t.getUser() != null ? t.getUser().getEmail() : "");
                map.put("status", t.getRentalStatus().name());
                return map;
            })
            .collect(Collectors.toList());

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("listing", mapListing(listing));
        response.put("applicants", applicants);

        if (listing.getTenant() != null) {
            Tenant t = listing.getTenant();
            Map<String, Object> tenantMap = new java.util.HashMap<>();
            tenantMap.put("id", t.getId());
            tenantMap.put("firstName", t.getFirstName());
            tenantMap.put("lastName", t.getLastName());
            tenantMap.put("phoneNumber", t.getPhoneNumber());
            tenantMap.put("email", t.getUser() != null ? t.getUser().getEmail() : "");
            tenantMap.put("status", t.getRentalStatus() != null ? t.getRentalStatus().name() : "RENTING");
            response.put("tenant", tenantMap);
        }

        return ResponseEntity.ok(response);
    }
}
