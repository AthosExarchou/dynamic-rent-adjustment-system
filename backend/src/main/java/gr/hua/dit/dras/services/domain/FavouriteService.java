package gr.hua.dit.dras.services.domain;

/* imports */
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.repositories.ListingRepository;
import gr.hua.dit.dras.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class FavouriteService {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;

    public FavouriteService(UserRepository userRepository, ListingRepository listingRepository) {
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
    }

    @Transactional(readOnly = true)
    public List<Listing> getFavouriteListings(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        return user.getFavouriteListings().stream()
                .sorted(Comparator.comparing(Listing::getId).reversed())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Set<Integer> getFavouriteListingIds(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        return user.getFavouriteListings().stream()
                .map(Listing::getId)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public boolean isFavourite(Integer userId, Integer listingId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        return user.getFavouriteListings().stream()
                .anyMatch(l -> Objects.equals(l.getId(), listingId));
    }

    public boolean addFavourite(Integer userId, Integer listingId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found with id: " + listingId));

        boolean added = user.getFavouriteListings().add(listing);
        if (added) {
            listing.getFavouritedByUsers().add(user);
            userRepository.save(user);
        }
        return true;
    }

    public boolean removeFavourite(Integer userId, Integer listingId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        Listing listing = listingRepository.findById(listingId).orElse(null);

        if (listing != null) {
            user.getFavouriteListings().remove(listing);
            listing.getFavouritedByUsers().remove(user);
            userRepository.save(user);
        } else {
            user.getFavouriteListings().removeIf(l -> Objects.equals(l.getId(), listingId));
            userRepository.save(user);
        }
        return true;
    }

    public Set<Integer> syncFavourites(Integer userId, List<Integer> listingIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        if (listingIds != null && !listingIds.isEmpty()) {
            List<Listing> listingsToAdd = listingRepository.findAllById(listingIds);
            for (Listing listing : listingsToAdd) {
                user.getFavouriteListings().add(listing);
                listing.getFavouritedByUsers().add(user);
            }
            userRepository.save(user);
        }
        return user.getFavouriteListings().stream()
                .map(Listing::getId)
                .collect(Collectors.toSet());
    }
}
