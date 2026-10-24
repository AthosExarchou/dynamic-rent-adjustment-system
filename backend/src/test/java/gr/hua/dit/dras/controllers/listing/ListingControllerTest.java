package gr.hua.dit.dras.controllers.listing;

/* imports */
import gr.hua.dit.dras.dto.PriceSuggestionDTO;
import gr.hua.dit.dras.dto.PriceSuggestionRequestDTO;
import gr.hua.dit.dras.model.enums.PropertyType;
import gr.hua.dit.dras.services.application.ListingApplicationService;
import gr.hua.dit.dras.services.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
}
