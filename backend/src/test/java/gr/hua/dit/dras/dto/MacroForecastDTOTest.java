package gr.hua.dit.dras.dto;

/* imports */
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.hua.dit.dras.dto.MacroForecastDTO.HistoricalQuarterItem;
import gr.hua.dit.dras.dto.MacroForecastDTO.QuarterlyForecastItem;
import gr.hua.dit.dras.dto.MacroForecastDTO.SyncMetadataInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MacroForecastDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesModelJsonAndIgnoresUnknownFields() throws Exception {
        String json = """
                {
                  "version": "2026.1",
                  "generatedAt": "2026-01-01",
                  "unknownTopLevel": 1,
                  "macroForecast": {
                    "modelStatus": "ACTIVE",
                    "unknownForecastField": true,
                    "recentHistoricalSeries": [{
                      "quarter": "2024Q1",
                      "date": "2024-03-31",
                      "priceIndex": 100.5,
                      "qoqGrowthPercentage": 1.2,
                      "yoyGrowthPercentage": 3.4,
                      "status": "historical",
                      "unknownItemField": "ignored"
                    }],
                    "quarterlyForecast": [{
                      "quarter": "2025Q1",
                      "date": "2025-03-31",
                      "predictedPriceIndex": 110.5,
                      "lowerBoundIndex": 105.0,
                      "upperBoundIndex": 115.0,
                      "qoqGrowthPercentage": 2.0,
                      "yoyGrowthPercentage": 4.0
                    }]
                  },
                  "syncMetadata": {
                    "status": "OK",
                    "activeFile": "macro.json",
                    "latestAvailableQuarter": "2024Q4",
                    "datasetPublicationDate": "2025-01-01",
                    "lastCheckedAt": "2025-01-02",
                    "checkIntervalDays": 7,
                    "unknownSyncField": "ignored"
                  }
                }
                """;

        MacroForecastDTO dto = objectMapper.readValue(json, MacroForecastDTO.class);

        HistoricalQuarterItem historical = dto.getMacroForecast().getRecentHistoricalSeries().get(0);
        QuarterlyForecastItem forecast = dto.getMacroForecast().getQuarterlyForecast().get(0);
        SyncMetadataInfo sync = dto.getSyncMetadata();

        assertAll(
                () -> assertEquals("2026.1", dto.getVersion()),
                () -> assertEquals("ACTIVE", dto.getMacroForecast().getModelStatus()),
                () -> assertEquals("2024Q1", historical.getQuarter()),
                () -> assertEquals(100.5, historical.getPriceIndex()),
                () -> assertEquals("historical", historical.getStatus()),
                () -> assertEquals("2025Q1", forecast.getQuarter()),
                () -> assertEquals(110.5, forecast.getPredictedPriceIndex()),
                () -> assertEquals(115.0, forecast.getUpperBoundIndex()),
                () -> assertEquals("OK", sync.getStatus()),
                () -> assertEquals("macro.json", sync.getActiveFile()),
                () -> assertEquals(7, sync.getCheckIntervalDays())
        );
    }

    @Test
    void missingSeriesDefaultToEmptyLists() throws Exception {
        MacroForecastDTO dto = objectMapper.readValue("{\"macroForecast\":{}}", MacroForecastDTO.class);

        assertAll(
                () -> assertNotNull(dto.getMacroForecast().getRecentHistoricalSeries()),
                () -> assertTrue(dto.getMacroForecast().getRecentHistoricalSeries().isEmpty()),
                () -> assertNotNull(dto.getMacroForecast().getQuarterlyForecast()),
                () -> assertTrue(dto.getMacroForecast().getQuarterlyForecast().isEmpty())
        );
    }
}
