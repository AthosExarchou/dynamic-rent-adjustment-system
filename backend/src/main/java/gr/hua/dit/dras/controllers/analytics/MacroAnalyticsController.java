package gr.hua.dit.dras.controllers.analytics;

/* imports */
import gr.hua.dit.dras.dto.MacroForecastDTO;
import gr.hua.dit.dras.services.domain.MacroAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/analytics", "/analytics"})
public class MacroAnalyticsController {

    private final MacroAnalyticsService macroAnalyticsService;

    public MacroAnalyticsController(MacroAnalyticsService macroAnalyticsService) {
        this.macroAnalyticsService = macroAnalyticsService;
    }

    /**
     * Retrieves the Bank of Greece Athens residential price index forecast and sync metadata.
     * Accessible to Platform Administrators and Property Owners.
     */
    @GetMapping("/macro-forecast")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'OWNER')")
    public ResponseEntity<MacroForecastDTO> getMacroForecast() {
        MacroForecastDTO forecast = macroAnalyticsService.getMacroForecast();
        return ResponseEntity.ok(forecast);
    }
}
