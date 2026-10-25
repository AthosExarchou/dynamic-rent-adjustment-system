package gr.hua.dit.dras.controllers.user;

/* imports */
import gr.hua.dit.dras.config.LoginSuccessHandler;
import gr.hua.dit.dras.config.SecurityConfig;
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.repositories.UserRepository;
import gr.hua.dit.dras.services.domain.FavouriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FavouriteController.class)
@Import(SecurityConfig.class)
class FavouriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FavouriteService favouriteService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private LoginSuccessHandler loginSuccessHandler;

    private User testUser;
    private Listing testListing;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "user@test.com", "Password123!");
        testUser.setId(1);

        testListing = new Listing();
        ReflectionTestUtils.setField(testListing, "id", 10);
        testListing.setTitle("Beautiful Apartment");
        testListing.setDescription("A nice place");
        testListing.setPrice(750);
        testListing.setPricePerM2(10);
        testListing.setAddress("Athens Center");
        testListing.setSizeM2(75);
        testListing.setStatus(ListingStatus.APPROVED);
    }

    @Test
    @DisplayName("Unauthenticated request to /favourites redirects to login")
    void getFavourites_Unauthenticated_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/favourites"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));

        verifyNoInteractions(favouriteService);
    }

    @Test
    @DisplayName("Authenticated user fetches favourite listings")
    @WithMockUser(username = "user@test.com")
    void getFavourites_Authenticated_ReturnsList() throws Exception {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(favouriteService.getFavouriteListings(1)).thenReturn(List.of(testListing));

        mockMvc.perform(get("/favourites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].title").value("Beautiful Apartment"))
                .andExpect(jsonPath("$[0].price").value(750));

        verify(favouriteService).getFavouriteListings(1);
    }

    @Test
    @DisplayName("Authenticated user fetches favourite IDs")
    @WithMockUser(username = "user@test.com")
    void getFavouriteIds_Authenticated_ReturnsSet() throws Exception {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(favouriteService.getFavouriteListingIds(1)).thenReturn(Set.of(10, 20));

        mockMvc.perform(get("/favourites/ids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(favouriteService).getFavouriteListingIds(1);
    }

    @Test
    @DisplayName("Authenticated user adds favourite listing")
    @WithMockUser(username = "user@test.com")
    void addFavourite_Authenticated_ReturnsSuccess() throws Exception {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(favouriteService.addFavourite(1, 10)).thenReturn(true);

        mockMvc.perform(post("/favourites/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.favourited").value(true))
                .andExpect(jsonPath("$.listingId").value(10));

        verify(favouriteService).addFavourite(1, 10);
    }

    @Test
    @DisplayName("Authenticated user removes favourite listing")
    @WithMockUser(username = "user@test.com")
    void removeFavourite_Authenticated_ReturnsSuccess() throws Exception {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(favouriteService.removeFavourite(1, 10)).thenReturn(true);

        mockMvc.perform(delete("/favourites/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.favourited").value(false))
                .andExpect(jsonPath("$.listingId").value(10));

        verify(favouriteService).removeFavourite(1, 10);
    }

    @Test
    @DisplayName("Authenticated user syncs favourite listings")
    @WithMockUser(username = "user@test.com")
    void syncFavourites_Authenticated_ReturnsUpdatedIds() throws Exception {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(favouriteService.syncFavourites(1, List.of(10, 20))).thenReturn(Set.of(10, 20));

        mockMvc.perform(post("/favourites/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"listingIds\": [10, 20]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.favouriteIds").isArray());

        verify(favouriteService).syncFavourites(1, List.of(10, 20));
    }
}
