package gr.hua.dit.dras.controllers.listing;

/* imports */
import gr.hua.dit.dras.dto.ExternalListingDTO;
import gr.hua.dit.dras.services.integration.ExternalListingImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/external-import")
public class ExternalImportController {

    private final ExternalListingImportService importService;

    public ExternalImportController(ExternalListingImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/listings")
    public ResponseEntity<String> importListings(@RequestBody List<ExternalListingDTO> dtos) {

        if (dtos == null || dtos.isEmpty()) {
            return ResponseEntity.badRequest().body("Payload is empty.");
        }

        int importedCount = importService.importExternalListings(dtos);

        return ResponseEntity.ok("Successfully imported/updated " +
                importedCount + " external listings.");
    }

}
