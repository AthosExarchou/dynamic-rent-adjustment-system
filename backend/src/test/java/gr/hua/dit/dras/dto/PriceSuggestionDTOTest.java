package gr.hua.dit.dras.dto;

/* imports */
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceSuggestionDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void newDtoHasDefaultRangeTypeAndAdjustments() {
        PriceSuggestionDTO dto = new PriceSuggestionDTO();

        assertAll(
                () -> assertEquals("ESTIMATED_MARKET_RANGE", dto.getPriceRangeType()),
                () -> assertNotNull(dto.getAppliedAdjustments()),
                () -> assertTrue(dto.getAppliedAdjustments().isEmpty())
        );
    }

    @Test
    void marginDescriptionRoundTrips() {
        PriceSuggestionDTO dto = new PriceSuggestionDTO();
        dto.setMarginDescription("Margin below the ARIMA estimate.");

        assertEquals("Margin below the ARIMA estimate.", dto.getMarginDescription());
    }

    @Test
    void nestedForecastAndBenchmarkUseExpectedJsonKeys() throws Exception {
        PriceSuggestionDTO.ArimaForecastInfo arima = new PriceSuggestionDTO.ArimaForecastInfo();
        arima.setModelStatus("ACTIVE");
        arima.setHorizonWeeks(12);
        PriceSuggestionDTO.MarketBenchmarkInfo benchmark = new PriceSuggestionDTO.MarketBenchmarkInfo();
        benchmark.setBenchmarkStatus("OK");
        benchmark.setComparableCount(4);

        PriceSuggestionDTO dto = new PriceSuggestionDTO();
        dto.setArimaForecast(arima);
        dto.setMarketBenchmark(benchmark);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(dto));

        assertAll(
                () -> assertEquals("ACTIVE", json.path("arimaForecast").path("modelStatus").asText()),
                () -> assertEquals(12, json.path("arimaForecast").path("horizonWeeks").asInt()),
                () -> assertEquals("OK", json.path("marketBenchmark").path("benchmarkStatus").asText()),
                () -> assertEquals(4, json.path("marketBenchmark").path("comparableCount").asInt())
        );
    }
}
