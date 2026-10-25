package gr.hua.dit.dras.services.domain;

/* imports */
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.repositories.ListingRepository;
import gr.hua.dit.dras.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavouriteServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ListingRepository listingRepository;

    @InjectMocks
    private FavouriteService favouriteService;

    private User testUser;
    private Listing listing1;
    private Listing listing2;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "Password123!");
        testUser.setId(1);

        listing1 = new Listing();
        ReflectionTestUtils.setField(listing1, "id", 10);
        listing1.setTitle("Apartment in Athens Center");

        listing2 = new Listing();
        ReflectionTestUtils.setField(listing2, "id", 20);
        listing2.setTitle("Modern Studio in Koukaki");
    }

    @Test
    @DisplayName("addFavourite: successfully adds listing to user favourites")
    void testAddFavouriteSuccess() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(listingRepository.findById(10)).thenReturn(Optional.of(listing1));

        boolean result = favouriteService.addFavourite(1, 10);

        assertThat(result).isTrue();
        assertThat(testUser.getFavouriteListings()).contains(listing1);
        assertThat(listing1.getFavouritedByUsers()).contains(testUser);
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("addFavourite: throws exception if user not found")
    void testAddFavouriteUserNotFound() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favouriteService.addFavourite(99, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("addFavourite: throws exception if listing not found")
    void testAddFavouriteListingNotFound() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(listingRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favouriteService.addFavourite(1, 999))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Listing not found");
    }

    @Test
    @DisplayName("removeFavourite: successfully removes listing from user favourites")
    void testRemoveFavouriteSuccess() {
        testUser.getFavouriteListings().add(listing1);
        listing1.getFavouritedByUsers().add(testUser);

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(listingRepository.findById(10)).thenReturn(Optional.of(listing1));

        boolean result = favouriteService.removeFavourite(1, 10);

        assertThat(result).isTrue();
        assertThat(testUser.getFavouriteListings()).doesNotContain(listing1);
        assertThat(listing1.getFavouritedByUsers()).doesNotContain(testUser);
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("removeFavourite: removes by id even if listing is already deleted from DB")
    void testRemoveFavouriteWhenListingNotInDB() {
        testUser.getFavouriteListings().add(listing1);

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(listingRepository.findById(10)).thenReturn(Optional.empty());

        boolean result = favouriteService.removeFavourite(1, 10);

        assertThat(result).isTrue();
        assertThat(testUser.getFavouriteListings()).doesNotContain(listing1);
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("getFavouriteListings: returns favourited listings ordered by id descending")
    void testGetFavouriteListings() {
        testUser.getFavouriteListings().add(listing1);
        testUser.getFavouriteListings().add(listing2);

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        List<Listing> favourites = favouriteService.getFavouriteListings(1);

        assertThat(favourites).hasSize(2);
        assertThat(favourites.get(0).getId()).isEqualTo(20);
        assertThat(favourites.get(1).getId()).isEqualTo(10);
    }

    @Test
    @DisplayName("getFavouriteListingIds: returns set of listing IDs")
    void testGetFavouriteListingIds() {
        testUser.getFavouriteListings().add(listing1);
        testUser.getFavouriteListings().add(listing2);

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        Set<Integer> ids = favouriteService.getFavouriteListingIds(1);

        assertThat(ids).containsExactlyInAnyOrder(10, 20);
    }

    @Test
    @DisplayName("isFavourite: returns true if listing is in favourites, false otherwise")
    void testIsFavourite() {
        testUser.getFavouriteListings().add(listing1);

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        assertThat(favouriteService.isFavourite(1, 10)).isTrue();
        assertThat(favouriteService.isFavourite(1, 20)).isFalse();
    }

    @Test
    @DisplayName("syncFavourites: adds all provided listing IDs to user favourites")
    void testSyncFavourites() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(listingRepository.findAllById(List.of(10, 20))).thenReturn(List.of(listing1, listing2));

        Set<Integer> syncedIds = favouriteService.syncFavourites(1, List.of(10, 20));

        assertThat(syncedIds).containsExactlyInAnyOrder(10, 20);
        assertThat(testUser.getFavouriteListings()).contains(listing1, listing2);
        verify(userRepository, times(1)).save(testUser);
    }
}
