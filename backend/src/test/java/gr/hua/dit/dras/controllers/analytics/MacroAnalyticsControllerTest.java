package gr.hua.dit.dras.controllers.analytics;

/* imports */
import gr.hua.dit.dras.config.LoginSuccessHandler;
import gr.hua.dit.dras.config.SecurityConfig;
import gr.hua.dit.dras.dto.MacroForecastDTO;
import gr.hua.dit.dras.services.domain.MacroAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MacroAnalyticsController.class)
@Import(SecurityConfig.class)
class MacroAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MacroAnalyticsService macroAnalyticsService;

    @MockitoBean
    private LoginSuccessHandler loginSuccessHandler;

    private MacroForecastDTO mockForecast;

    @BeforeEach
    void setUp() {
        mockForecast = new MacroForecastDTO();
        mockForecast.setVersion("1.0");
        mockForecast.setRegion("Athens");
        mockForecast.setDataSource("Bank of Greece Open Data (Dataset 5)");

        MacroForecastDTO.MacroForecastInfo info = new MacroForecastDTO.MacroForecastInfo();
        info.setModelStatus("SARIMA");
        info.setLatestHistoricalQuarter("2026-Q2");
        info.setLatestHistoricalIndex(118.3);
        info.setProjectedEndIndex(128.56);
        info.setMacroTrend("UPWARD");
        info.setProjectedAnnualGrowthRate(4.25);
        info.setHorizonQuarters(8);
        mockForecast.setMacroForecast(info);

        when(macroAnalyticsService.getMacroForecast()).thenReturn(mockForecast);
    }

    @Test
    @DisplayName("Unauthenticated request to /api/analytics/macro-forecast is rejected (redirect or 401)")
    void getMacroForecast_Unauthenticated_Rejected() throws Exception {
        mockMvc.perform(get("/api/analytics/macro-forecast")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("User with TENANT authority is forbidden (403)")
    @WithMockUser(authorities = "TENANT")
    void getMacroForecast_TenantAuthority_Forbidden() throws Exception {
        mockMvc.perform(get("/api/analytics/macro-forecast")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("User with ADMIN authority succeeds (200) and returns macro forecast")
    @WithMockUser(authorities = "ADMIN")
    void getMacroForecast_AdminAuthority_Success() throws Exception {
        mockMvc.perform(get("/api/analytics/macro-forecast")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.0"))
                .andExpect(jsonPath("$.region").value("Athens"))
                .andExpect(jsonPath("$.macroForecast.macroTrend").value("UPWARD"))
                .andExpect(jsonPath("$.macroForecast.latestHistoricalIndex").value(118.3));
    }

    @Test
    @DisplayName("User with OWNER authority succeeds (200) and returns macro forecast")
    @WithMockUser(authorities = "OWNER")
    void getMacroForecast_OwnerAuthority_Success() throws Exception {
        mockMvc.perform(get("/api/analytics/macro-forecast")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.0"))
                .andExpect(jsonPath("$.macroForecast.latestHistoricalQuarter").value("2026-Q2"));
    }
}
