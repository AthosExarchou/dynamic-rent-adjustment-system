package gr.hua.dit.dras.controllers.admin;

/* imports */
import gr.hua.dit.dras.dto.SystemStatusDTO;
import gr.hua.dit.dras.services.domain.AdminSystemService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/admin/system", "/admin/system"})
public class AdminSystemController {

    private final AdminSystemService adminSystemService;

    public AdminSystemController(AdminSystemService adminSystemService) {
        this.adminSystemService = adminSystemService;
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
}
