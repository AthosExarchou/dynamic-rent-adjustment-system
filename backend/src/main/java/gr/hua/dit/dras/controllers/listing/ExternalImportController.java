package gr.hua.dit.dras.controllers.listing;

/* imports */
import gr.hua.dit.dras.dto.ExternalListingDTO;
import gr.hua.dit.dras.services.integration.ExternalListingImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public ResponseEntity<?> importListings(
            @RequestBody @Size(max = 500, message = "Batch size cannot exceed 500 listings") List<ExternalListingDTO> dtos) {

        if (dtos == null || dtos.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Payload is empty."));
        }

        int importedCount = importService.importExternalListings(dtos);

        /* Structured summary lets the scraper verify that rows were actually persisted */
        Map<String, Integer> summary = new LinkedHashMap<>();
        summary.put("received", dtos.size());
        summary.put("imported", importedCount);
        summary.put("skipped", dtos.size() - importedCount);

        return ResponseEntity.ok(summary);
    }

}
