package gr.hua.dit.dras.controllers.listing;

/* imports */
import gr.hua.dit.dras.dto.ExternalListingDTO;
import gr.hua.dit.dras.services.integration.ExternalListingImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import java.util.List;

@RestController
@RequestMapping("/external-import")
@Validated
public class ExternalImportController {

    private final ExternalListingImportService importService;

    public ExternalImportController(ExternalListingImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/listings")
    /* @Valid intentionally omitted: ExternalListingImportService performs per-DTO
    validation and skips invalid entries instead of failing the entire batch. */
    public ResponseEntity<String> importListings(
            @RequestBody @Size(max = 500, message = "Batch size cannot exceed 500 listings") List<ExternalListingDTO> dtos) {

        if (dtos == null || dtos.isEmpty()) {
            return ResponseEntity.badRequest().body("Payload is empty.");
        }

        int importedCount = importService.importExternalListings(dtos);

        return ResponseEntity.ok("Successfully imported/updated " +
                importedCount + " external listings.");
    }

}
