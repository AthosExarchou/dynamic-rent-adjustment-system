package gr.hua.dit.dras.controllers.listing;

/* imports */
import gr.hua.dit.dras.dto.PriceSuggestionDTO;
import gr.hua.dit.dras.dto.PriceSuggestionRequestDTO;
import gr.hua.dit.dras.entities.*;
import gr.hua.dit.dras.model.enums.PropertyType;
import gr.hua.dit.dras.services.application.ListingApplicationService;
import gr.hua.dit.dras.services.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private ListingService listingService;
    @Mock
    private OwnerService ownerService;
    @Mock
    private TenantService tenantService;
    @Mock
    private ListingApplicationService listingApplicationService;
    @Mock
    private PriceSuggestionService priceSuggestionService;
    @Mock
    private ImageStorageService imageStorageService;

    @Spy
    private ListingImagePolicy imagePolicy = new ListingImagePolicy(false, "target/test-images");

    @Spy
    private ListingResponseMapper listingMapper = new ListingResponseMapper(imagePolicy);

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private ListingController listingController;

    private PriceSuggestionDTO mockSuggestion;

    @BeforeEach
    void setUp() {
        mockSuggestion = new PriceSuggestionDTO();
        mockSuggestion.setSuggestedPrice(850);
        mockSuggestion.setSuggestedPricePerM2(11.33);
        mockSuggestion.setMinPrice(780);
        mockSuggestion.setMaxPrice(920);
    }

    @Test
    @DisplayName("Should return 200 OK with price suggestion when request is valid")
    void testGetPriceSuggestionSuccess() {
        PriceSuggestionRequestDTO request = new PriceSuggestionRequestDTO();
        request.setSizeM2(75);
        request.setPropertyType(PropertyType.APARTMENT);

        when(bindingResult.hasErrors()).thenReturn(false);
        when(priceSuggestionService.suggestPrice(request)).thenReturn(mockSuggestion);

        ResponseEntity<?> response = listingController.getPriceSuggestion(request, bindingResult);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockSuggestion);
        verify(priceSuggestionService, times(1)).suggestPrice(request);
    }

    @Test
    @DisplayName("Should return 400 Bad Request when validation errors exist")
    void testGetPriceSuggestionValidationError() {
        PriceSuggestionRequestDTO request = new PriceSuggestionRequestDTO();

        when(bindingResult.hasErrors()).thenReturn(true);
        when(bindingResult.getAllErrors()).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = listingController.getPriceSuggestion(request, bindingResult);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(priceSuggestionService, never()).suggestPrice(any());
    }

    @Test
    @DisplayName("Should return public listings list")
    void testShowListings() {
        when(listingService.getListings()).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = listingController.showListings();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(listingService, times(1)).getListings();
    }

    @Test
    @DisplayName("Should return owner listings when calling myListings")
    void testMyListings() {
        when(listingApplicationService.getOwnerListingsForCurrentUser()).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = listingController.myListings();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(listingApplicationService, times(1)).getOwnerListingsForCurrentUser();
    }

    @Test
    @DisplayName("Should disable listing successfully")
    void testDisableListing() {
        ResponseEntity<?> response = listingController.disableListing(100);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(listingApplicationService, times(1)).disableListing(100);
    }

    @Test
    @DisplayName("Should enable listing successfully")
    void testEnableListing() {
        ResponseEntity<?> response = listingController.enableListing(100);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(listingApplicationService, times(1)).enableListing(100);
    }

    @Test
    @DisplayName("Should toggle listing status successfully")
    void testToggleListingStatus() {
        ResponseEntity<?> response = listingController.toggleListingStatus(100);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(listingApplicationService, times(1)).toggleListingStatus(100);
    }

    @Test
    @DisplayName("Should view applications with applicant notes and metadata")
    void testViewApplications() {
        Listing listing = new Listing();
        org.springframework.test.util.ReflectionTestUtils.setField(listing, "id", 100);
        listing.setTitle("Athens Flat");
        listing.setDescription("Great flat");
        listing.setPrice(500);
        listing.setPricePerM2(10);
        listing.setAddress("Athens 123");
        listing.setSizeM2(50);

        Tenant tenant = new Tenant("Jane", "Doe", "+306912345678");
        org.springframework.test.util.ReflectionTestUtils.setField(tenant, "id", 10);
        User user = new User();
        user.setEmail("jane@example.com");
        tenant.setUser(user);

        listing.addApplicant(tenant, "I work from home and need high-speed internet");

        when(listingApplicationService.viewApplications(100)).thenReturn(listing);

        ResponseEntity<?> response = listingController.viewApplications(100);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        java.util.Map<?, ?> body = (java.util.Map<?, ?>) response.getBody();
        assertThat(body).isNotNull();
        java.util.List<?> applicants = (java.util.List<?>) body.get("applicants");
        assertThat(applicants).hasSize(1);
        java.util.Map<?, ?> applicantMap = (java.util.Map<?, ?>) applicants.get(0);
        assertThat(applicantMap.get("firstName")).isEqualTo("Jane");
        assertThat(applicantMap.get("lastName")).isEqualTo("Doe");
        assertThat(applicantMap.get("notes")).isEqualTo("I work from home and need high-speed internet");
        assertThat(applicantMap.get("appliedAt")).isNotNull();
    }
}
