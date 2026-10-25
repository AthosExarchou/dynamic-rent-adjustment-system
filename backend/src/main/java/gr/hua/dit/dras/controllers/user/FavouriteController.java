package gr.hua.dit.dras.controllers.user;

/* imports */
import gr.hua.dit.dras.controllers.listing.ListingController;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.repositories.UserRepository;
import gr.hua.dit.dras.services.domain.FavouriteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/favourites")
public class FavouriteController {

    private final FavouriteService favouriteService;
    private final UserRepository userRepository;

    public FavouriteController(FavouriteService favouriteService, UserRepository userRepository) {
        this.favouriteService = favouriteService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }

    @GetMapping
    public ResponseEntity<?> getFavourites(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required to view favourites"));
        }
        List<Map<String, Object>> favourites = favouriteService.getFavouriteListings(user.getId())
                .stream()
                .map(ListingController::mapListing)
                .collect(Collectors.toList());
        return ResponseEntity.ok(favourites);
    }

    @GetMapping("/ids")
    public ResponseEntity<?> getFavouriteIds(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required"));
        }
        Set<Integer> ids = favouriteService.getFavouriteListingIds(user.getId());
        return ResponseEntity.ok(ids);
    }

    @PostMapping("/{listingId}")
    public ResponseEntity<?> addFavourite(@PathVariable Integer listingId, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required to add favourite"));
        }
        favouriteService.addFavourite(user.getId(), listingId);
        return ResponseEntity.ok(Map.of("success", true, "listingId", listingId, "favourited", true));
    }

    @DeleteMapping("/{listingId}")
    public ResponseEntity<?> removeFavourite(@PathVariable Integer listingId, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required to remove favourite"));
        }
        favouriteService.removeFavourite(user.getId(), listingId);
        return ResponseEntity.ok(Map.of("success", true, "listingId", listingId, "favourited", false));
    }

    @PostMapping("/sync")
    public ResponseEntity<?> syncFavourites(
            @RequestBody(required = false) Map<String, List<Integer>> payload, Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required to sync favourites"));
        }
        List<Integer> listingIds = payload != null ? payload.get("listingIds") : Collections.emptyList();
        Set<Integer> updatedIds = favouriteService.syncFavourites(user.getId(), listingIds);

        return ResponseEntity.ok(Map.of("success", true, "favouriteIds", updatedIds));
    }
}
