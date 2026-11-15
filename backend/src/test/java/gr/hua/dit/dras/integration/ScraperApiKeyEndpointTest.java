package gr.hua.dit.dras.integration;

/* imports */
import gr.hua.dit.dras.config.ScraperApiKeyFilter;
import gr.hua.dit.dras.repositories.ListingRepository;
import gr.hua.dit.dras.services.infrastructure.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the scraper authentication contract end to end, mirroring the
 * manual curl checks used to validate a deployment:
 *
 * <ul>
 *   <li>correct key through the nginx/routing prefix</li>
 *   <li>correct key straight to the backend</li>
 *   <li>wrong key</li>
 *   <li>no key</li>
 *   <li>correct key with a real listing</li>
 * </ul>
 */
@SpringBootTest(properties = "app.scraper.api-key=test-scraper-key")
@AutoConfigureMockMvc
@Transactional
class ScraperApiKeyEndpointTest {

    private static final String SCRAPER_KEY = "test-scraper-key";
    private static final String IMPORT_PATH = "/external-import/listings";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ListingRepository listingRepository;

    @MockitoBean
    private EmailService emailService;

    private static String singleListing(String sourceUrl) {
        return String.format("""
                [
                    {
                        "title": "Scraper Imported Listing",
                        "description": "Pushed by the analytics scraper with a service API key.",
                        "price": 700,
                        "pricePerM2": 14,
                        "address": "Kypseli 10, Athens",
                        "sizeM2": 50,
                        "sourceUrl": "%s"
                    }
                ]
                """, sourceUrl);
    }

    private static String uniqueSourceUrl(String suffix) {
        return "https://www.spitogatos.gr/aggelia/" + System.nanoTime() + "_" + suffix;
    }

    @Test
    @DisplayName("Correct key through the /api context path imports a listing (nginx route)")
    void correctKey_ThroughContextPath_ImportsListing() throws Exception {
        String sourceUrl = uniqueSourceUrl("context_path");

        mockMvc.perform(post("/api" + IMPORT_PATH)
                        .contextPath("/api")
                        .header(ScraperApiKeyFilter.HEADER_NAME, SCRAPER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleListing(sourceUrl)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(1))
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skipped").value(0));

        assertThat(listingRepository.findBySourceUrl(sourceUrl)).isPresent();
    }

    @Test
    @DisplayName("Correct key straight to the backend imports a listing (no context path)")
    void correctKey_DirectToBackend_ImportsListing() throws Exception {
        String sourceUrl = uniqueSourceUrl("direct");

        mockMvc.perform(post(IMPORT_PATH)
                        .header(ScraperApiKeyFilter.HEADER_NAME, SCRAPER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleListing(sourceUrl)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(1))
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skipped").value(0));

        assertThat(listingRepository.findBySourceUrl(sourceUrl)).isPresent();
    }

    @Test
    @DisplayName("Correct key with an empty payload returns 400 and imports nothing")
    void correctKey_EmptyPayload_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post(IMPORT_PATH)
                        .header(ScraperApiKeyFilter.HEADER_NAME, SCRAPER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Payload is empty."));
    }

    @Test
    @DisplayName("Wrong key returns 401 and imports nothing")
    void wrongKey_ReturnsUnauthorized() throws Exception {
        String sourceUrl = uniqueSourceUrl("wrong_key");

        mockMvc.perform(post(IMPORT_PATH)
                        .header(ScraperApiKeyFilter.HEADER_NAME, "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleListing(sourceUrl)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid or unconfigured X-Scraper-Key"));

        assertThat(listingRepository.findBySourceUrl(sourceUrl)).isEmpty();
    }

    @Test
    @DisplayName("No key is rejected with 403 and imports nothing")
    void missingKey_IsRejected() throws Exception {
        String sourceUrl = uniqueSourceUrl("missing_key");

        mockMvc.perform(post(IMPORT_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleListing(sourceUrl)))
                .andExpect(status().isForbidden());

        assertThat(listingRepository.findBySourceUrl(sourceUrl)).isEmpty();
    }

    @Test
    @DisplayName("The scraper key does not authenticate requests outside the import namespace")
    void scraperKey_DoesNotAuthorizeOtherEndpoints() throws Exception {
        /* /listings/mylisting requires an authenticated session; the key must not provide one */
        mockMvc.perform(get("/listings/mylisting")
                        .header(ScraperApiKeyFilter.HEADER_NAME, SCRAPER_KEY))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
