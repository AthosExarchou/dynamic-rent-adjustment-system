package gr.hua.dit.dras.controllers.listing;

/* imports */
import gr.hua.dit.dras.config.LoginSuccessHandler;
import gr.hua.dit.dras.config.SecurityConfig;
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.services.application.ListingApplicationService;
import gr.hua.dit.dras.services.domain.ListingService;
import gr.hua.dit.dras.services.domain.OwnerService;
import gr.hua.dit.dras.services.domain.PriceSuggestionService;
import gr.hua.dit.dras.services.domain.TenantService;
import gr.hua.dit.dras.services.domain.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ListingController.class)
@Import(SecurityConfig.class)
class ListingControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListingService listingService;

    @MockitoBean
    private ListingApplicationService listingApplicationService;

    @MockitoBean
    private OwnerService ownerService;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PriceSuggestionService priceSuggestionService;

    @MockitoBean
    private LoginSuccessHandler loginSuccessHandler;

    private Listing createTestListing(Integer id, String title, ListingStatus status) {
        Listing listing = new Listing();
        ReflectionTestUtils.setField(listing, "id", id);
        listing.setTitle(title);
        listing.setStatus(status);
        listing.setPrice(600);
        listing.setPricePerM2(10);
        listing.setAddress("Athens 123");
        return listing;
    }

    @Test
    @DisplayName("Public GET /listings is accessible unauthenticated")
    void getListings_Unauthenticated_ReturnsOk() throws Exception {
        Listing listing = createTestListing(1, "Public Listing", ListingStatus.APPROVED);
        when(listingService.getListings()).thenReturn(List.of(listing));

        mockMvc.perform(get("/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Public Listing"));
    }

    @Test
    @DisplayName("GET /listings/forapproval unauthenticated redirects to login")
    void getForApproval_Unauthenticated_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/listings/forapproval"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));

        verifyNoInteractions(listingService);
    }

    @Test
    @DisplayName("GET /listings/forapproval non-admin returns 403 Forbidden")
    @WithMockUser(authorities = "USER")
    void getForApproval_NonAdmin_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/listings/forapproval"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(listingService);
    }

    @Test
    @DisplayName("GET /listings/forapproval admin returns 200 OK")
    @WithMockUser(authorities = "ADMIN")
    void getForApproval_Admin_ReturnsOk() throws Exception {
        Listing listing = createTestListing(2, "Pending Listing", ListingStatus.PENDING);
        when(listingService.getPendingListings()).thenReturn(List.of(listing));

        mockMvc.perform(get("/listings/forapproval"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].title").value("Pending Listing"));
    }

    @Test
    @DisplayName("POST /listings/approve/{id} by admin returns 200 OK")
    @WithMockUser(authorities = "ADMIN")
    void approveListing_Admin_ReturnsOk() throws Exception {
        mockMvc.perform(post("/listings/approve/5")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(listingApplicationService).approveListing(5);
    }

    @Test
    @DisplayName("POST /listings/approve/{id} without CSRF returns 403 Forbidden")
    @WithMockUser(authorities = "ADMIN")
    void approveListing_WithoutCsrf_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/listings/approve/5"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(listingApplicationService);
    }

    @Test
    @DisplayName("POST /listings/reject/{id} by admin returns 200 OK")
    @WithMockUser(authorities = "ADMIN")
    void rejectListing_Admin_ReturnsOk() throws Exception {
        mockMvc.perform(post("/listings/reject/5")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(listingApplicationService).rejectListing(5);
    }

    @Test
    @DisplayName("GET /listings/mylisting by authenticated owner returns 200 OK")
    @WithMockUser(authorities = "OWNER")
    void myListings_AuthenticatedOwner_ReturnsOk() throws Exception {
        Listing listing = createTestListing(10, "Owner Prop", ListingStatus.APPROVED);
        when(listingApplicationService.getOwnerListingsForCurrentUser()).thenReturn(List.of(listing));

        mockMvc.perform(get("/listings/mylisting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].title").value("Owner Prop"));
    }
}
