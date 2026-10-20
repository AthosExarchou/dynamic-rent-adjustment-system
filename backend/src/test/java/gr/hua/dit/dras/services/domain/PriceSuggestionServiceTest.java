package gr.hua.dit.dras.services.domain;

/* imports */
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.hua.dit.dras.dto.PriceSuggestionDTO;
import gr.hua.dit.dras.dto.PriceSuggestionRequestDTO;
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.model.enums.PropertyType;
import gr.hua.dit.dras.repositories.ListingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceSuggestionServiceTest {

    @Mock
    private ListingRepository listingRepository;

    private PriceSuggestionService priceSuggestionService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        priceSuggestionService = new PriceSuggestionService(listingRepository, objectMapper);
        priceSuggestionService.init();
    }

    @Test
    @DisplayName("Should calculate a valid price suggestion for a standard apartment")
    void testStandardApartmentPriceSuggestion() {
        when(listingRepository.findByExternalTrue()).thenReturn(Collections.emptyList());

        PriceSuggestionRequestDTO request = new PriceSuggestionRequestDTO();
        request.setSizeM2(75);
        request.setPropertyType(PropertyType.APARTMENT);
        request.setAddress("Athens");

        PriceSuggestionDTO result = priceSuggestionService.suggestPrice(request);

        assertNotNull(result);
        assertNotNull(result.getSuggestedPrice());
        assertTrue(result.getSuggestedPrice() > 0);
        assertTrue(result.getMinPrice() < result.getSuggestedPrice());
        assertTrue(result.getMaxPrice() > result.getSuggestedPrice());
        assertNotNull(result.getArimaForecast());
        assertEquals("Athens", result.getArimaForecast().getPredictedPricePerM2() > 0 ? "Athens" : "");
        assertNotNull(result.getMarketBenchmark());
        assertTrue(result.getMarketBenchmark().getComparableCount() > 0);
        assertNotNull(result.getExplanation());
    }

    @Test
    @DisplayName("Should reject invalid or non-positive sizes")
    void testInvalidSizeThrowsException() {
        PriceSuggestionRequestDTO request = new PriceSuggestionRequestDTO();
        request.setSizeM2(0);

        assertThrows(IllegalArgumentException.class, () -> priceSuggestionService.suggestPrice(request));
        assertThrows(IllegalArgumentException.class, () -> priceSuggestionService.suggestPrice(null));
    }

    @Test
    @DisplayName("Should adjust price based on floor and construction year")
    void testMicroAdjustmentsFloorAndYearBuilt() {
        when(listingRepository.findByExternalTrue()).thenReturn(Collections.emptyList());

        PriceSuggestionRequestDTO baselineRequest = new PriceSuggestionRequestDTO();
        baselineRequest.setSizeM2(80);
        baselineRequest.setPropertyType(PropertyType.APARTMENT);

        PriceSuggestionDTO baseline = priceSuggestionService.suggestPrice(baselineRequest);

        PriceSuggestionRequestDTO premiumRequest = new PriceSuggestionRequestDTO();
        premiumRequest.setSizeM2(80);
        premiumRequest.setPropertyType(PropertyType.APARTMENT);
        premiumRequest.setFloor(4);
        premiumRequest.setYearBuilt(2020);

        PriceSuggestionDTO premium = priceSuggestionService.suggestPrice(premiumRequest);

        assertTrue(premium.getSuggestedPrice() > baseline.getSuggestedPrice());
        assertTrue(premium.getAppliedAdjustments().size() >= 2);
    }

    @Test
    @DisplayName("Should apply prime location premium for Kolonaki")
    void testLocationAdjustments() {
        when(listingRepository.findByExternalTrue()).thenReturn(Collections.emptyList());

        PriceSuggestionRequestDTO genericRequest = new PriceSuggestionRequestDTO();
        genericRequest.setSizeM2(70);
        genericRequest.setAddress("Unknown Location");

        PriceSuggestionDTO generic = priceSuggestionService.suggestPrice(genericRequest);

        PriceSuggestionRequestDTO primeRequest = new PriceSuggestionRequestDTO();
        primeRequest.setSizeM2(70);
        primeRequest.setAddress("Kolonaki, Athens");

        PriceSuggestionDTO prime = priceSuggestionService.suggestPrice(primeRequest);

        assertTrue(prime.getSuggestedPrice() > generic.getSuggestedPrice());
    }

    @Test
    @DisplayName("Should blend live external listings from the database")
    void testLiveDatabaseListingsIntegration() {
        Listing scraped1 = new Listing();
        scraped1.setPropertyType(PropertyType.APARTMENT);
        scraped1.setPrice(1200);
        scraped1.setSizeM2(60);
        scraped1.setPricePerM2(20);
        scraped1.setExternal(true);

        when(listingRepository.findByExternalTrue()).thenReturn(List.of(scraped1));

        PriceSuggestionRequestDTO request = new PriceSuggestionRequestDTO();
        request.setSizeM2(60);
        request.setPropertyType(PropertyType.APARTMENT);

        PriceSuggestionDTO result = priceSuggestionService.suggestPrice(request);

        assertNotNull(result);
        assertTrue(result.getMarketBenchmark().getDataSource().contains("Live DRAS external database"));
    }

    @Test
    @DisplayName("Should adjust price based on bedrooms layout and multiple bathrooms")
    void testBedroomsAndBathroomsAdjustments() {
        when(listingRepository.findByExternalTrue()).thenReturn(Collections.emptyList());

        PriceSuggestionRequestDTO baselineRequest = new PriceSuggestionRequestDTO();
        baselineRequest.setSizeM2(90);
        baselineRequest.setPropertyType(PropertyType.APARTMENT);
        baselineRequest.setBedrooms(1);
        baselineRequest.setBathrooms(1);

        PriceSuggestionDTO baseline = priceSuggestionService.suggestPrice(baselineRequest);

        PriceSuggestionRequestDTO luxuryRequest = new PriceSuggestionRequestDTO();
        luxuryRequest.setSizeM2(90);
        luxuryRequest.setPropertyType(PropertyType.APARTMENT);
        luxuryRequest.setBedrooms(3);
        luxuryRequest.setBathrooms(2);

        PriceSuggestionDTO luxury = priceSuggestionService.suggestPrice(luxuryRequest);

        assertTrue(luxury.getSuggestedPrice() > baseline.getSuggestedPrice());
        assertTrue(luxury.getAppliedAdjustments().stream().anyMatch(a -> a.contains("bedrooms")));
        assertTrue(luxury.getAppliedAdjustments().stream().anyMatch(a -> a.contains("baths")));
    }

    @Test
    @DisplayName("Should populate explicit price range metadata and model status")
    void testPriceRangeMetadataAndModelStatus() {
        when(listingRepository.findByExternalTrue()).thenReturn(Collections.emptyList());

        PriceSuggestionRequestDTO request = new PriceSuggestionRequestDTO();
        request.setSizeM2(65);
        request.setPropertyType(PropertyType.APARTMENT);

        PriceSuggestionDTO result = priceSuggestionService.suggestPrice(request);

        assertEquals("ESTIMATED_MARKET_RANGE", result.getPriceRangeType());
        assertNotNull(result.getMarginPercentage());
        assertTrue(result.getMarginPercentage() >= 8.0 && result.getMarginPercentage() <= 15.0);
        assertNotNull(result.getArimaForecast().getModelStatus());
        assertNotNull(result.getMarketBenchmark().getBenchmarkStatus());
    }
}
