package gr.hua.dit.dras.controllers.listing;

/* imports */
import gr.hua.dit.dras.config.LoginSuccessHandler;
import gr.hua.dit.dras.config.SecurityConfig;
import gr.hua.dit.dras.services.integration.ExternalListingImportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExternalImportController.class)
@Import(SecurityConfig.class)
class ExternalImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExternalListingImportService importService;

    @MockitoBean
    private LoginSuccessHandler loginSuccessHandler;

    @Test
    @DisplayName("Unauthenticated request to /external-import/listings is redirected to login")
    void importListings_Unauthenticated_RedirectsToLogin() throws Exception {
        mockMvc.perform(post("/external-import/listings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));

        verifyNoInteractions(importService);
    }

    @Test
    @DisplayName("Non-admin user to /external-import/listings returns 403 Forbidden")
    @WithMockUser(authorities = "USER")
    void importListings_NonAdminUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/external-import/listings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(importService);
    }

    @Test
    @DisplayName("Admin user without CSRF token returns 403 Forbidden")
    @WithMockUser(authorities = "ADMIN")
    void importListings_WithoutCsrf_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/external-import/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(importService);
    }

    @Test
    @DisplayName("Admin user with empty payload returns 400 Bad Request")
    @WithMockUser(authorities = "ADMIN")
    void importListings_EmptyPayload_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/external-import/listings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Payload is empty."));

        verifyNoInteractions(importService);
    }

    @Test
    @DisplayName("Admin user with valid payload successfully imports listings")
    @WithMockUser(authorities = "ADMIN")
    void importListings_ValidPayload_ReturnsOk() throws Exception {
        when(importService.importExternalListings(anyList())).thenReturn(1);

        String jsonPayload = """
                [
                    {
                        "title": "Modern Studio in Panormou",
                        "description": "Bright and renovated studio flat near Panormou metro station.",
                        "price": 550,
                        "address": "Panormou 12, Athens",
                        "sizeM2": 45,
                        "yearBuilt": 2010,
                        "sourceUrl": "https://example.com/listing/123"
                    }
                ]
                """;

        mockMvc.perform(post("/external-import/listings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully imported/updated 1 external listings."));

        verify(importService).importExternalListings(anyList());
    }

    @Test
    @DisplayName("Admin user with batch size exceeding 500 returns 400 Bad Request")
    @WithMockUser(authorities = "ADMIN")
    void importListings_BatchSizeExceedsLimit_ReturnsBadRequest() throws Exception {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 501; i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format("""
                    {
                        "title": "Listing %d",
                        "description": "Description",
                        "price": 500,
                        "address": "Address %d",
                        "sizeM2": 50,
                        "sourceUrl": "https://example.com/%d"
                    }
                    """, i, i, i));
        }
        sb.append("]");

        mockMvc.perform(post("/external-import/listings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sb.toString()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(importService);
    }
}
