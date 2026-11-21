package gr.hua.dit.dras.controllers.admin;

/* imports */
import gr.hua.dit.dras.dto.ExternalCleanupResult;
import gr.hua.dit.dras.dto.SystemStatusDTO;
import gr.hua.dit.dras.services.application.ExternalListingCleanupService;
import gr.hua.dit.dras.services.domain.AdminSystemService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/admin/system", "/admin/system"})
public class AdminSystemController {

    private final AdminSystemService adminSystemService;
    private final ExternalListingCleanupService externalListingCleanupService;

    public AdminSystemController(
            AdminSystemService adminSystemService,
            ExternalListingCleanupService externalListingCleanupService
    ) {
        this.adminSystemService = adminSystemService;
        this.externalListingCleanupService = externalListingCleanupService;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<SystemStatusDTO> getSystemStatus() {
        SystemStatusDTO status = adminSystemService.getSystemStatus();
        return ResponseEntity.ok(status);
    }

    @PostMapping("/reload-forecast")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<SystemStatusDTO> reloadForecast() {
        adminSystemService.reloadForecastData();
        SystemStatusDTO status = adminSystemService.getSystemStatus();
        return ResponseEntity.ok(status);
    }

    @PostMapping("/cleanup-external-listings")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ExternalCleanupResult> cleanupExternalListings(
            @RequestParam(defaultValue = "30") int graceDays,
            @RequestParam(defaultValue = "false") boolean dryRun
    ) {
        return ResponseEntity.ok(externalListingCleanupService.cleanup(graceDays, dryRun));
    }
}
