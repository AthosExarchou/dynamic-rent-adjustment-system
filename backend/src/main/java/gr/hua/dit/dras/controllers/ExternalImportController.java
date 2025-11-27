package gr.hua.dit.dras.controllers;

/* imports */
import gr.hua.dit.dras.dto.ExternalListingDTO;
import gr.hua.dit.dras.services.ExternalListingImportService;
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
    public String importListings(@RequestBody List<ExternalListingDTO> dtos) {
        importService.importExternalListings(dtos);
        return "Imported " + dtos.size() + " external listings.";
    }

}
