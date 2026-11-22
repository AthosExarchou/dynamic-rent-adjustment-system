package gr.hua.dit.dras.integration;

/* imports */
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.model.enums.PropertyType;
import gr.hua.dit.dras.model.enums.RentalDuration;
import gr.hua.dit.dras.repositories.ListingRepository;
import gr.hua.dit.dras.repositories.OwnerRepository;
import gr.hua.dit.dras.services.infrastructure.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.scraper.api-key=test-scraper-key",
        "dras.external-images.enabled=false"
})
@AutoConfigureMockMvc
@Transactional
class ExternalImportImagesDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @MockitoBean
    private EmailService emailService;

    @Test
    @DisplayName("Production import clears scraped image references from new and existing listings")
    @WithMockUser(authorities = "ADMIN")
    void importPipeline_DiscardsImagesWhenDisabled() throws Exception {
        String sourceUrl = "https://spitogatos.gr/listing/" + System.currentTimeMillis() + "_no_images";
        Owner systemOwner = ownerRepository.findBySystemOwnerTrue().orElseThrow();

        Listing existing = new Listing();
        existing.setTitle("Legacy imported listing");
        existing.setDescription("Legacy description");
        existing.setPrice(600);
        existing.setPricePerM2(10);
        existing.setAddress("Athens 1");
        existing.setSizeM2(60);
        existing.setPropertyType(PropertyType.APARTMENT);
        existing.setRentalDuration(RentalDuration.LONG_TERM);
        existing.setSourceUrl(sourceUrl);
        existing.setExternal(true);
        existing.setOwner(systemOwner);
        existing.setImages(new ArrayList<>(List.of("https://images.spitogatos.gr/legacy.jpg")));
        listingRepository.saveAndFlush(existing);

        String batchJson = """
                [
                    {
                        "title": "Imported without images",
                        "description": "Production drops scraped image data.",
                        "price": 700,
                        "pricePerM2": 12,
                        "address": "Athens 2",
                        "sizeM2": 60,
                        "sourceUrl": "%s",
                        "images": ["data/images/123/abc.jpg"]
                    }
                ]
                """.formatted(sourceUrl);

        mockMvc.perform(post("/external-import/listings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1));

        Listing saved = listingRepository.findBySourceUrl(sourceUrl).orElseThrow();
        assertThat(saved.getImages()).isEmpty();
    }

    @Test
    @DisplayName("Read responses hide remote image URLs from existing rows")
    @WithMockUser(authorities = "ADMIN")
    void readResponse_FiltersRemoteImageUrls() throws Exception {
        Owner systemOwner = ownerRepository.findBySystemOwnerTrue().orElseThrow();

        Listing external = new Listing();
        external.setTitle("Legacy external listing");
        external.setDescription("Old scraped URLs must not reach the UI.");
        external.setPrice(600);
        external.setPricePerM2(10);
        external.setAddress("Athens 1");
        external.setSizeM2(60);
        external.setPropertyType(PropertyType.APARTMENT);
        external.setRentalDuration(RentalDuration.LONG_TERM);
        external.setExternal(true);
        external.setOwner(systemOwner);
        external.setImages(new ArrayList<>(List.of("https://images.spitogatos.gr/legacy.jpg")));
        listingRepository.saveAndFlush(external);

        Listing local = new Listing();
        local.setTitle("Owner listing with stale URL");
        local.setDescription("Remote URLs are filtered even on local rows.");
        local.setPrice(700);
        local.setPricePerM2(12);
        local.setAddress("Athens 2");
        local.setSizeM2(65);
        local.setPropertyType(PropertyType.APARTMENT);
        local.setRentalDuration(RentalDuration.LONG_TERM);
        local.setOwner(systemOwner);
        local.setImages(new ArrayList<>(List.of(
                "https://cdn.example.com/stale.jpg",
                "/images/demo/image1.jpg"
        )));
        listingRepository.saveAndFlush(local);

        mockMvc.perform(get("/listings/{id}", external.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images").isEmpty());

        mockMvc.perform(get("/listings/{id}", local.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images[0]").value("/images/demo/image1.jpg"))
                .andExpect(jsonPath("$.images[1]").doesNotExist());
    }
}
