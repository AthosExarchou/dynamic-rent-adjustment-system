package gr.hua.dit.dras.dto;

/* imports */
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemStatusDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static SystemStatusDTO status() {
        return new SystemStatusDTO(
                "macro.json",
                "2026-01-01T00:00:00",
                "2 hours ago",
                true,
                "prices.json",
                "2026-01-02T00:00:00",
                "1 day ago",
                false,
                "2026-01-03T00:00:00",
                "3 hours ago",
                101L,
                102L,
                103L,
                104L,
                10L,
                20L,
                30L,
                40L,
                50L,
                60L
        );
    }

    @Test
    void constructorMapsAllTwentyArgumentsToGetters() {
        SystemStatusDTO dto = status();

        assertAll(
                () -> assertEquals("macro.json", dto.getMacroPredictionsFile()),
                () -> assertEquals("2026-01-01T00:00:00", dto.getMacroPredictionsLastModified()),
                () -> assertEquals("2 hours ago", dto.getMacroPredictionsAge()),
                () -> assertTrue(dto.isMacroPredictionsLoaded()),
                () -> assertEquals("prices.json", dto.getPricePredictionsFile()),
                () -> assertEquals("2026-01-02T00:00:00", dto.getPricePredictionsLastModified()),
                () -> assertEquals("1 day ago", dto.getPricePredictionsAge()),
                () -> assertFalse(dto.isPricePredictionsLoaded()),
                () -> assertEquals("2026-01-03T00:00:00", dto.getLastReloadedTimestamp()),
                () -> assertEquals("3 hours ago", dto.getLastReloadedAge()),
                () -> assertEquals(101L, dto.getTotalUsers()),
                () -> assertEquals(102L, dto.getTotalOwners()),
                () -> assertEquals(103L, dto.getTotalTenants()),
                () -> assertEquals(104L, dto.getTotalListings()),
                () -> assertEquals(10L, dto.getLocalListings()),
                () -> assertEquals(20L, dto.getExternalListings()),
                () -> assertEquals(30L, dto.getActiveListings()),
                () -> assertEquals(40L, dto.getPendingListings()),
                () -> assertEquals(50L, dto.getRentedListings()),
                () -> assertEquals(60L, dto.getRejectedListings())
        );
    }

    @Test
    void jsonRoundTripUsesNoArgConstructorAndSetters() throws Exception {
        String json = objectMapper.writeValueAsString(status());

        SystemStatusDTO dto = objectMapper.readValue(json, SystemStatusDTO.class);

        assertAll(
                () -> assertEquals("macro.json", dto.getMacroPredictionsFile()),
                () -> assertTrue(dto.isMacroPredictionsLoaded()),
                () -> assertFalse(dto.isPricePredictionsLoaded()),
                () -> assertEquals("3 hours ago", dto.getLastReloadedAge()),
                () -> assertEquals(101L, dto.getTotalUsers()),
                () -> assertEquals(10L, dto.getLocalListings()),
                () -> assertEquals(20L, dto.getExternalListings()),
                () -> assertEquals(30L, dto.getActiveListings()),
                () -> assertEquals(40L, dto.getPendingListings()),
                () -> assertEquals(50L, dto.getRentedListings()),
                () -> assertEquals(60L, dto.getRejectedListings())
        );
    }
}
