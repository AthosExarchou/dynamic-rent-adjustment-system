package gr.hua.dit.dras.integration;

/* imports */
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.model.enums.ListingStatus;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExternalImportPipelineIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @MockitoBean
    private EmailService emailService;

    @Test
    @DisplayName("Batch import external listings persists records linked to systemOwner with auto-approval and images")
    @WithMockUser(authorities = "ADMIN")
    void importPipeline_PersistsLinkedToSystemOwner() throws Exception {
        String sourceUrl1 = "https://spitogatos.gr/listing/" + System.currentTimeMillis() + "_1";
        String sourceUrl2 = "https://spitogatos.gr/listing/" + System.currentTimeMillis() + "_2";

        String batchJson = String.format("""
                [
                    {
                        "title": "Modern Apartment in Pangrati",
                        "description": "Fully renovated with balcony and AC units.",
                        "price": 750,
                        "pricePerM2": 11,
                        "address": "Pangrati 45, Athens",
                        "sizeM2": 70,
                        "yearBuilt": 2015,
                        "propertyType": "διαμέρισμα",
                        "rentalDuration": "LONG_TERM",
                        "sourceUrl": "%s",
                        "images": ["https://images.spitogatos.gr/img1.jpg", "https://images.spitogatos.gr/img2.jpg"]
                    },
                    {
                        "title": "Cozy Studio Near Syntagma",
                        "description": "Walk to metro, quiet street.",
                        "price": 500,
                        "pricePerM2": 14,
                        "address": "Syntagma 10, Athens",
                        "sizeM2": 35,
                        "yearBuilt": 2008,
                        "propertyType": "στούντιο",
                        "rentalDuration": "SHORT_TERM",
                        "sourceUrl": "%s"
                    }
                ]
                """, sourceUrl1, sourceUrl2);

        // Post batch as Admin
        mockMvc.perform(post("/external-import/listings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchJson))
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully imported/updated 2 external listings."));

        // Verify database records
        Optional<Owner> systemOwnerOpt = ownerRepository.findBySystemOwnerTrue();
        assertThat(systemOwnerOpt).isPresent();
        Owner systemOwner = systemOwnerOpt.get();

        Optional<Listing> listing1Opt = listingRepository.findBySourceUrl(sourceUrl1);
        assertThat(listing1Opt).isPresent();
        Listing listing1 = listing1Opt.get();
        assertThat(listing1.isExternal()).isTrue();
        assertThat(listing1.getStatus()).isEqualTo(ListingStatus.APPROVED);
        assertThat(listing1.getOwner().getId()).isEqualTo(systemOwner.getId());
        assertThat(listing1.getPropertyType()).isEqualTo(PropertyType.APARTMENT);
        assertThat(listing1.getRentalDuration()).isEqualTo(RentalDuration.LONG_TERM);
        assertThat(listing1.getPrice()).isEqualTo(750);
        assertThat(listing1.getSizeM2()).isEqualTo(70);
        assertThat(listing1.getPricePerM2()).isEqualTo(11);
        assertThat(listing1.getAddress()).isEqualTo("Pangrati 45, Athens");
        assertThat(listing1.getYearBuilt()).isEqualTo(2015);
        assertThat(listing1.getDateScraped()).isNotNull();
        assertThat(listing1.getImages()).containsExactly(
                "https://images.spitogatos.gr/img1.jpg",
                "https://images.spitogatos.gr/img2.jpg"
        );

        Optional<Listing> listing2Opt = listingRepository.findBySourceUrl(sourceUrl2);
        assertThat(listing2Opt).isPresent();
        Listing listing2 = listing2Opt.get();
        assertThat(listing2.isExternal()).isTrue();
        assertThat(listing2.getStatus()).isEqualTo(ListingStatus.APPROVED);
        assertThat(listing2.getOwner().getId()).isEqualTo(systemOwner.getId());
        assertThat(listing2.getPropertyType()).isEqualTo(PropertyType.STUDIO);
        assertThat(listing2.getRentalDuration()).isEqualTo(RentalDuration.SHORT_TERM);
        assertThat(listing2.getPrice()).isEqualTo(500);
        assertThat(listing2.getSizeM2()).isEqualTo(35);

        // Re-importing with updated price tests upsert idempotency
        String updateBatchJson = String.format("""
                [
                    {
                        "title": "Modern Apartment in Pangrati (Updated)",
                        "description": "Fully renovated with balcony and AC units.",
                        "price": 800,
                        "pricePerM2": 12,
                        "address": "Pangrati 45, Athens",
                        "sizeM2": 70,
                        "yearBuilt": 2015,
                        "propertyType": "διαμέρισμα",
                        "rentalDuration": "LONG_TERM",
                        "sourceUrl": "%s"
                    }
                ]
                """, sourceUrl1);

        mockMvc.perform(post("/external-import/listings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBatchJson))
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully imported/updated 1 external listings."));

        Listing updatedListing1 = listingRepository.findBySourceUrl(sourceUrl1).orElseThrow();
        assertThat(updatedListing1.getId()).isEqualTo(listing1.getId()); // Same entity
        assertThat(updatedListing1.getPrice()).isEqualTo(800);
        assertThat(updatedListing1.getTitle()).isEqualTo("Modern Apartment in Pangrati (Updated)");
    }

    @Test
    @DisplayName("Batch import calculates missing pricePerM2 fallback")
    @WithMockUser(authorities = "ADMIN")
    void importPipeline_CalculatesMissingPricePerM2() throws Exception {
        String url = "https://spitogatos.gr/listing/" + System.currentTimeMillis() + "_calc";

        String json = String.format("""
                [
                    {
                        "title": "Listing Without Explicit PricePerM2",
                        "description": "Calculates ppm2 from price and size.",
                        "price": 600,
                        "address": "Athens Center 12",
                        "sizeM2": 60,
                        "yearBuilt": 2012,
                        "sourceUrl": "%s"
                    }
                ]
                """, url);

        mockMvc.perform(post("/external-import/listings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        Listing saved = listingRepository.findBySourceUrl(url).orElseThrow();
        // 600 / 60 = 10
        assertThat(saved.getPricePerM2()).isEqualTo(10);
    }

    @Test
    @DisplayName("Re-importing a disabled listing preserves DISABLED status")
    @WithMockUser(authorities = "ADMIN")
    void importPipeline_PreservesDisabledStatusOnReimport() throws Exception {
        String url = "https://spitogatos.gr/listing/" + System.currentTimeMillis() + "_disabled";

        String initialJson = String.format("""
                [
                    {
                        "title": "Listing to be disabled",
                        "description": "Initial description",
                        "price": 500,
                        "address": "Street 1",
                        "sizeM2": 50,
                        "sourceUrl": "%s"
                    }
                ]
                """, url);

        mockMvc.perform(post("/external-import/listings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initialJson))
                .andExpect(status().isOk());

        Listing listing = listingRepository.findBySourceUrl(url).orElseThrow();
        listing.disable();
        listingRepository.saveAndFlush(listing);

        // Re-import
        mockMvc.perform(post("/external-import/listings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initialJson))
                .andExpect(status().isOk());

        Listing reimported = listingRepository.findBySourceUrl(url).orElseThrow();
        assertThat(reimported.getStatus()).isEqualTo(ListingStatus.DISABLED);
    }

    @Test
    @DisplayName("Batch import skips invalid entries and processes valid ones")
    @WithMockUser(authorities = "ADMIN")
    void importPipeline_SkipsInvalidEntriesInBatch() throws Exception {
        String validUrl = "https://spitogatos.gr/listing/" + System.currentTimeMillis() + "_valid";

        // One invalid (missing sourceUrl, invalid yearBuilt 1500) and one valid entry
        String batchJson = String.format("""
                [
                    {
                        "title": "Invalid Listing Missing Source",
                        "description": "Invalid year and missing URL",
                        "price": 400,
                        "address": "Nowhere 1",
                        "sizeM2": 40,
                        "yearBuilt": 1500
                    },
                    {
                        "title": "Valid Independent Listing",
                        "description": "Good description.",
                        "price": 600,
                        "address": "Valid St 10",
                        "sizeM2": 60,
                        "yearBuilt": 2018,
                        "sourceUrl": "%s"
                    }
                ]
                """, validUrl);

        mockMvc.perform(post("/external-import/listings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchJson))
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully imported/updated 1 external listings."));

        assertThat(listingRepository.findBySourceUrl(validUrl)).isPresent();
    }
}
