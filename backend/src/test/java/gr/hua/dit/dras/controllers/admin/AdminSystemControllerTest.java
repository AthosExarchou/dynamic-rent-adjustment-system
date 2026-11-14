package gr.hua.dit.dras.controllers.admin;

import gr.hua.dit.dras.config.LoginSuccessHandler;
import gr.hua.dit.dras.config.SecurityConfig;
import gr.hua.dit.dras.dto.SystemStatusDTO;
import gr.hua.dit.dras.services.domain.AdminSystemService;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminSystemController.class)
@Import(SecurityConfig.class)
class AdminSystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminSystemService adminSystemService;

    @MockitoBean
    private LoginSuccessHandler loginSuccessHandler;

    private SystemStatusDTO mockStatus;

    @BeforeEach
    void setUp() {
        mockStatus = new SystemStatusDTO(
                "/app/analytics/outputs/macro_predictions.json",
                "2026-11-14T09:00:00Z",
                "2 hours ago",
                true,
                "/app/analytics/outputs/price_predictions.json",
                "2026-11-14T09:00:00Z",
                "2 hours ago",
                true,
                25L,
                10L,
                12L,
                30L,
                18L,
                12L,
                20L,
                5L,
                3L,
                2L
        );
        when(adminSystemService.getSystemStatus()).thenReturn(mockStatus);
    }

    @Test
    @DisplayName("GET /api/admin/system/status allows ADMIN and returns system status")
    @WithMockUser(authorities = {"ADMIN"})
    void testGetSystemStatus_Admin() throws Exception {
        mockMvc.perform(get("/api/admin/system/status")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalListings").value(30))
                .andExpect(jsonPath("$.localListings").value(18))
                .andExpect(jsonPath("$.externalListings").value(12))
                .andExpect(jsonPath("$.totalUsers").value(25))
                .andExpect(jsonPath("$.macroPredictionsLoaded").value(true));
    }

    @Test
    @DisplayName("GET /api/admin/system/status forbids non-ADMIN users")
    @WithMockUser(authorities = {"USER"})
    void testGetSystemStatus_NonAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/system/status")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/admin/system/reload-forecast allows ADMIN and triggers reload")
    @WithMockUser(authorities = {"ADMIN"})
    void testReloadForecast_Admin() throws Exception {
        mockMvc.perform(post("/api/admin/system/reload-forecast")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalListings").value(30));

        verify(adminSystemService).reloadForecastData();
    }

    @Test
    @DisplayName("POST /api/admin/system/reload-forecast forbids non-ADMIN users")
    @WithMockUser(authorities = {"USER"})
    void testReloadForecast_NonAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/system/reload-forecast")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
