package gr.hua.dit.dras.controllers.listing;

/* imports */
import gr.hua.dit.dras.config.LoginSuccessHandler;
import gr.hua.dit.dras.config.SecurityConfig;
import gr.hua.dit.dras.dto.ListingCreateDTO;
import gr.hua.dit.dras.dto.ListingFilterDTO;
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.model.enums.PropertyType;
import gr.hua.dit.dras.services.application.ListingApplicationService;
import gr.hua.dit.dras.services.domain.ListingService;
import gr.hua.dit.dras.services.domain.OwnerService;
import gr.hua.dit.dras.services.domain.PriceSuggestionService;
import gr.hua.dit.dras.services.domain.TenantService;
import gr.hua.dit.dras.services.domain.UserService;
import gr.hua.dit.dras.web.exception.RestExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
@Import({SecurityConfig.class, RestExceptionHandler.class})
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
    @DisplayName("POST /listings/reject/{id} with reason exceeding 500 characters returns 400 Bad Request")
    @WithMockUser(authorities = "ADMIN")
    void rejectListing_Admin_WithReasonExceeding500Characters_ReturnsBadRequest() throws Exception {
        String longReason = "a".repeat(501);

        mockMvc.perform(post("/listings/reject/5")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"" + longReason + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Rejection reason cannot exceed 500 characters"));

        verifyNoInteractions(listingApplicationService);
    }

    @Test
    @DisplayName("POST /listings/reject/{id} by admin with JSON reason returns 200 OK")
    @WithMockUser(authorities = "ADMIN")
    void rejectListing_Admin_WithJsonReason_ReturnsOk() throws Exception {
        mockMvc.perform(post("/listings/reject/5")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Blurry photos and price too high\"}"))
                .andExpect(status().isOk());

        verify(listingApplicationService).rejectListing(5, "Blurry photos and price too high");
    }

    @Test
    @DisplayName("POST /listings/reject/{id} by admin with request param reason returns 200 OK")
    @WithMockUser(authorities = "ADMIN")
    void rejectListing_Admin_WithParamReason_ReturnsOk() throws Exception {
        mockMvc.perform(post("/listings/reject/5")
                        .with(csrf())
                        .param("reason", "Incomplete address details"))
                .andExpect(status().isOk());

        verify(listingApplicationService).rejectListing(5, "Incomplete address details");
    }

    @Test
    @DisplayName("POST /listings/reject/{id} without reason returns 400 Bad Request")
    @WithMockUser(authorities = "ADMIN")
    void rejectListing_Admin_WithoutReason_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/listings/reject/5")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Rejection reason is required"));

        verifyNoInteractions(listingApplicationService);
    }

    @Test
    @DisplayName("POST /listings/reject/{id} with blank reason returns 400 Bad Request")
    @WithMockUser(authorities = "ADMIN")
    void rejectListing_Admin_WithBlankReason_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/listings/reject/5")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Rejection reason is required"));

        verifyNoInteractions(listingApplicationService);
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

    /* Filter & Search (/listings/filter) Endpoint Tests */

    @Test
    @DisplayName("GET /listings/filter unauthenticated returns 200 OK and mapped listings")
    void filterListings_Unauthenticated_ReturnsOkAndMappedListings() throws Exception {
        Listing listing = createTestListing(1, "Modern Studio", ListingStatus.APPROVED);
        listing.setSubtitle("Near Metro");
        listing.setDescription("Great location");
        listing.setPropertyType(PropertyType.STUDIO);
        listing.setBedrooms(1);
        listing.setBathrooms(1);
        listing.setSizeM2(35);
        listing.setYearBuilt(2010);
        listing.setFloor(2);

        when(listingService.filterListings(any(ListingFilterDTO.class))).thenReturn(List.of(listing));

        mockMvc.perform(get("/listings/filter"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Modern Studio"))
                .andExpect(jsonPath("$[0].subtitle").value("Near Metro"))
                .andExpect(jsonPath("$[0].propertyType").value("STUDIO"))
                .andExpect(jsonPath("$[0].price").value(600))
                .andExpect(jsonPath("$[0].bedrooms").value(1))
                .andExpect(jsonPath("$[0].bathrooms").value(1))
                .andExpect(jsonPath("$[0].sizeM2").value(35))
                .andExpect(jsonPath("$[0].yearBuilt").value(2010))
                .andExpect(jsonPath("$[0].floor").value(2))
                .andExpect(jsonPath("$[0].status").value("APPROVED"));
    }

    @Test
    @DisplayName("GET /listings/filter with all query parameters correctly binds to ListingFilterDTO")
    void filterListings_WithAllQueryParams_BindsToFilterDtoAndReturnsOk() throws Exception {
        Listing listing = createTestListing(5, "Luxury Penthouse", ListingStatus.APPROVED);
        when(listingService.filterListings(any(ListingFilterDTO.class))).thenReturn(List.of(listing));

        mockMvc.perform(get("/listings/filter")
                        .param("search", "Penthouse")
                        .param("title", "Luxury")
                        .param("minPrice", "500")
                        .param("maxPrice", "2000")
                        .param("type", "APARTMENT")
                        .param("municipality", "Athens")
                        .param("district", "Kolonaki")
                        .param("minBedrooms", "2")
                        .param("maxBedrooms", "4")
                        .param("minBathrooms", "1")
                        .param("maxBathrooms", "3")
                        .param("minYear", "1990")
                        .param("maxYear", "2024")
                        .param("updatedAfter", "2024-01-01")
                        .param("updatedBefore", "2024-12-31")
                        .param("externalOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].title").value("Luxury Penthouse"));

        ArgumentCaptor<ListingFilterDTO> captor = ArgumentCaptor.forClass(ListingFilterDTO.class);
        verify(listingService).filterListings(captor.capture());

        ListingFilterDTO captured = captor.getValue();
        assertThat(captured.getSearch()).isEqualTo("Penthouse");
        assertThat(captured.getTitle()).isEqualTo("Luxury");
        assertThat(captured.getMinPrice()).isEqualTo(500);
        assertThat(captured.getMaxPrice()).isEqualTo(2000);
        assertThat(captured.getType()).isEqualTo(PropertyType.APARTMENT);
        assertThat(captured.getMunicipality()).isEqualTo("Athens");
        assertThat(captured.getDistrict()).isEqualTo("Kolonaki");
        assertThat(captured.getMinBedrooms()).isEqualTo(2);
        assertThat(captured.getMaxBedrooms()).isEqualTo(4);
        assertThat(captured.getMinBathrooms()).isEqualTo(1);
        assertThat(captured.getMaxBathrooms()).isEqualTo(3);
        assertThat(captured.getMinYear()).isEqualTo(1990);
        assertThat(captured.getMaxYear()).isEqualTo(2024);
        assertThat(captured.getUpdatedAfter()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(captured.getUpdatedBefore()).isEqualTo(LocalDate.of(2024, 12, 31));
        assertThat(captured.getExternalOnly()).isTrue();
    }

    @Test
    @DisplayName("GET /listings/filter when no listings match returns empty JSON array")
    void filterListings_WhenEmpty_ReturnsEmptyArray() throws Exception {
        when(listingService.filterListings(any(ListingFilterDTO.class))).thenReturn(List.of());

        mockMvc.perform(get("/listings/filter"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /listings/filter returns 400 when service throws IllegalArgumentException")
    void filterListings_ServiceThrowsIllegalArgumentException_ReturnsBadRequest() throws Exception {
        when(listingService.filterListings(any(ListingFilterDTO.class)))
                .thenThrow(new IllegalArgumentException("Invalid price range."));

        mockMvc.perform(get("/listings/filter")
                        .param("minPrice", "1000")
                        .param("maxPrice", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid price range."));
    }

    @Test
    @DisplayName("GET /listings/filter with negative minPrice returns 400 Bad Request")
    void filterListings_NegativeMinPrice_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/listings/filter").param("minPrice", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));

        verifyNoInteractions(listingService);
    }

    @Test
    @DisplayName("GET /listings/filter with maxPrice exceeding limit returns 400 Bad Request")
    void filterListings_ExceedingMaxPrice_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/listings/filter").param("maxPrice", "20001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));

        verifyNoInteractions(listingService);
    }

    @Test
    @DisplayName("GET /listings/filter with negative minBedrooms returns 400 Bad Request")
    void filterListings_NegativeMinBedrooms_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/listings/filter").param("minBedrooms", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));

        verifyNoInteractions(listingService);
    }

    @Test
    @DisplayName("GET /listings/filter with maxBedrooms > 10 returns 400 Bad Request")
    void filterListings_ExceedingMaxBedrooms_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/listings/filter").param("maxBedrooms", "11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));

        verifyNoInteractions(listingService);
    }

    @Test
    @DisplayName("GET /listings/filter with negative minBathrooms returns 400 Bad Request")
    void filterListings_NegativeMinBathrooms_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/listings/filter").param("minBathrooms", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));

        verifyNoInteractions(listingService);
    }

    @Test
    @DisplayName("GET /listings/filter with maxBathrooms > 5 returns 400 Bad Request")
    void filterListings_ExceedingMaxBathrooms_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/listings/filter").param("maxBathrooms", "6"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));

        verifyNoInteractions(listingService);
    }

    @Test
    @DisplayName("GET /listings/filter with minYear < 1800 returns 400 Bad Request")
    void filterListings_InvalidMinYear_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/listings/filter").param("minYear", "1799"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));

        verifyNoInteractions(listingService);
    }

    @Test
    @DisplayName("GET /listings/filter with future updatedAfter returns 400 Bad Request")
    void filterListings_FutureUpdatedAfter_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/listings/filter").param("updatedAfter", "2099-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));

        verifyNoInteractions(listingService);
    }

    @Test
    @DisplayName("GET /listings/filter with future updatedBefore returns 400 Bad Request")
    void filterListings_FutureUpdatedBefore_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/listings/filter").param("updatedBefore", "2099-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));

        verifyNoInteractions(listingService);
    }

    @Test
    @DisplayName("GET /listings/filter with title exceeding 150 chars returns 400 Bad Request")
    void filterListings_ExceedingTitleLength_ReturnsBadRequest() throws Exception {
        String longTitle = "A".repeat(151);
        mockMvc.perform(get("/listings/filter").param("title", longTitle))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));

        verifyNoInteractions(listingService);
    }

    @Test
    @DisplayName("GET /listings/filter with search exceeding 150 chars returns 400 Bad Request")
    void filterListings_ExceedingSearchLength_ReturnsBadRequest() throws Exception {
        String longSearch = "A".repeat(151);
        mockMvc.perform(get("/listings/filter").param("search", longSearch))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));

        verifyNoInteractions(listingService);
    }

    @Test
    @DisplayName("POST /listings/edit/{id} by authorized OWNER returns 200 OK")
    @WithMockUser(authorities = "OWNER")
    void editListing_AuthorizedOwner_ReturnsOk() throws Exception {
        Listing updatedListing = createTestListing(10, "Updated Apartment", ListingStatus.PENDING);
        when(listingApplicationService.editListing(eq(10), any(ListingCreateDTO.class))).thenReturn(updatedListing);

        mockMvc.perform(post("/listings/edit/10")
                        .with(csrf())
                        .param("title", "Updated Apartment")
                        .param("description", "A renovated modern flat")
                        .param("price", "700")
                        .param("pricePerM2", "14")
                        .param("address", "Patision 100")
                        .param("propertyType", "APARTMENT")
                        .param("rentalDuration", "LONG_TERM")
                        .param("sizeM2", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Updated Apartment"));

        verify(listingApplicationService).editListing(eq(10), any(ListingCreateDTO.class));
    }

    @Test
    @DisplayName("POST /listings/edit/{id} with missing required fields returns 400 Bad Request")
    @WithMockUser(authorities = "OWNER")
    void editListing_ValidationFailure_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/listings/edit/10")
                        .with(csrf())
                        .param("title", "") // blank title violates @NotBlank
                        .param("price", "-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));

        verifyNoInteractions(listingApplicationService);
    }

    @Test
    @DisplayName("POST /listings/edit/{id} unauthenticated redirects to login")
    void editListing_Unauthenticated_RedirectsToLogin() throws Exception {
        mockMvc.perform(post("/listings/edit/10")
                        .with(csrf())
                        .param("title", "New Title"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));

        verifyNoInteractions(listingApplicationService);
    }

    @Test
    @DisplayName("POST /listings/new by ADMIN returns 403 Forbidden")
    @WithMockUser(authorities = {"ADMIN", "USER"})
    void saveListing_Admin_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/listings/new")
                        .with(csrf())
                        .param("title", "New Admin Apartment")
                        .param("description", "Admin apartment")
                        .param("price", "700")
                        .param("pricePerM2", "14")
                        .param("address", "Patision 100")
                        .param("propertyType", "APARTMENT")
                        .param("rentalDuration", "LONG_TERM")
                        .param("sizeM2", "50"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(listingApplicationService);
    }

    @Test
    @DisplayName("POST /listings/edit/{id} by ADMIN returns 200 OK")
    @WithMockUser(authorities = "ADMIN")
    void editListing_AuthorizedAdmin_ReturnsOk() throws Exception {
        Listing updatedListing = createTestListing(10, "Admin Edited Apartment", ListingStatus.APPROVED);
        when(listingApplicationService.editListing(eq(10), any(ListingCreateDTO.class))).thenReturn(updatedListing);

        mockMvc.perform(post("/listings/edit/10")
                        .with(csrf())
                        .param("title", "Admin Edited Apartment")
                        .param("description", "A renovated modern flat")
                        .param("price", "800")
                        .param("pricePerM2", "16")
                        .param("address", "Patision 100")
                        .param("propertyType", "APARTMENT")
                        .param("rentalDuration", "LONG_TERM")
                        .param("sizeM2", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Admin Edited Apartment"));

        verify(listingApplicationService).editListing(eq(10), any(ListingCreateDTO.class));
    }

    @Test
    @DisplayName("POST /listings/edit/{id} by non-owner returns 403 Forbidden")
    @WithMockUser(authorities = "OWNER")
    void editListing_NonOwner_ReturnsForbidden() throws Exception {
        when(listingApplicationService.editListing(eq(10), any(ListingCreateDTO.class)))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Not owner of this listing"));

        mockMvc.perform(post("/listings/edit/10")
                        .with(csrf())
                        .param("title", "Updated Apartment")
                        .param("description", "A modern flat")
                        .param("price", "700")
                        .param("pricePerM2", "14")
                        .param("address", "Patision 100")
                        .param("propertyType", "APARTMENT")
                        .param("rentalDuration", "LONG_TERM")
                        .param("sizeM2", "50"))
                .andExpect(status().isForbidden());
    }
}
