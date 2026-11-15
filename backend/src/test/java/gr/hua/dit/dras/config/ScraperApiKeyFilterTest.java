package gr.hua.dit.dras.config;

/* imports */
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the service-to-service authentication filter.
 *
 * <p>These cover the behaviour that cannot be expressed through MockMvc alone:
 * fail-closed handling of an unconfigured server key, preservation of existing
 * sessions, and the fact that an anonymous authentication must not be mistaken
 * for a logged-in user.</p>
 */
class ScraperApiKeyFilterTest {

    private static final String CONFIGURED_KEY = "test-scraper-key";
    private static final String IMPORT_PATH = "/external-import/listings";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static MockHttpServletRequest request(String requestUri, String contextPath, String providedKey) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", requestUri);
        request.setContextPath(contextPath);
        if (providedKey != null) {
            request.addHeader(ScraperApiKeyFilter.HEADER_NAME, providedKey);
        }
        return request;
    }

    private record Invocation(MockHttpServletResponse response, MockFilterChain chain) {
    }

    private static Invocation invoke(MockHttpServletRequest request, String configuredKey) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        new ScraperApiKeyFilter(configuredKey).doFilter(request, response, chain);
        return new Invocation(response, chain);
    }

    @Test
    @DisplayName("Filter only applies to the external import namespace")
    void shouldNotFilter_OnlyAppliesToImportPath() {
        ScraperApiKeyFilter filter = new ScraperApiKeyFilter(CONFIGURED_KEY);

        assertThat(filter.shouldNotFilter(request("/listings", "", null))).isTrue();
        assertThat(filter.shouldNotFilter(request("/auth/login", "", null))).isTrue();

        assertThat(filter.shouldNotFilter(request(IMPORT_PATH, "", null))).isFalse();
    }

    @Test
    @DisplayName("Path resolution strips the servlet context path")
    void shouldNotFilter_HandlesContextPath() {
        ScraperApiKeyFilter filter = new ScraperApiKeyFilter(CONFIGURED_KEY);

        assertThat(filter.shouldNotFilter(request("/api" + IMPORT_PATH, "/api", null))).isFalse();
        assertThat(filter.shouldNotFilter(request("/api/listings", "/api", null))).isTrue();
    }

    @Test
    @DisplayName("Valid key authenticates the request with ADMIN authority")
    void doFilter_ValidKey_AuthenticatesAsAdmin() throws Exception {
        Invocation invocation = invoke(request(IMPORT_PATH, "", CONFIGURED_KEY), CONFIGURED_KEY);

        assertThat(invocation.chain().getRequest()).isNotNull();
        assertThat(invocation.response().getStatus()).isEqualTo(200);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ADMIN");
    }

    @Test
    @DisplayName("Valid key is accepted when the backend runs under a context path")
    void doFilter_ValidKeyWithContextPath_AuthenticatesAsAdmin() throws Exception {
        Invocation invocation = invoke(request("/api" + IMPORT_PATH, "/api", CONFIGURED_KEY), CONFIGURED_KEY);

        assertThat(invocation.chain().getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    @DisplayName("Wrong key is rejected with 401 and never reaches the controller")
    void doFilter_WrongKey_ReturnsUnauthorized() throws Exception {
        Invocation invocation = invoke(request(IMPORT_PATH, "", "wrong-key"), CONFIGURED_KEY);

        assertThat(invocation.response().getStatus()).isEqualTo(401);
        assertThat(invocation.response().getContentAsString())
                .contains("Invalid or unconfigured X-Scraper-Key");
        assertThat(invocation.chain().getRequest()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Unconfigured server key fails closed even when a key is supplied")
    void doFilter_UnconfiguredServerKey_FailsClosed() throws Exception {
        for (String configuredKey : new String[]{"", null}) {
            Invocation invocation = invoke(request(IMPORT_PATH, "", CONFIGURED_KEY), configuredKey);

            assertThat(invocation.response().getStatus())
                    .as("configured key: <%s>", configuredKey)
                    .isEqualTo(401);
            assertThat(invocation.chain().getRequest()).isNull();
        }
    }

    @Test
    @DisplayName("Missing key falls through so authorization rules can reject the request")
    void doFilter_MissingKey_FallsThroughWithoutAuthenticating() throws Exception {
        Invocation invocation = invoke(request(IMPORT_PATH, "", null), CONFIGURED_KEY);

        assertThat(invocation.chain().getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("An existing session is never overridden by the scraper filter")
    void doFilter_ExistingAuthentication_IsPreserved() throws Exception {
        UsernamePasswordAuthenticationToken admin = UsernamePasswordAuthenticationToken.authenticated(
                "admin@example.com", null, List.of(new SimpleGrantedAuthority("ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(admin);

        /* A stale or wrong header from a browser request must not downgrade the session */
        Invocation invocation = invoke(request(IMPORT_PATH, "", "wrong-key"), CONFIGURED_KEY);

        assertThat(invocation.chain().getRequest()).isNotNull();
        assertThat(invocation.response().getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(admin);
    }

    @Test
    @DisplayName("Anonymous authentication is not treated as an authenticated session")
    void doFilter_AnonymousAuthentication_StillValidatesKey() throws Exception {
        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
                "anonymous-key",
                "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        Invocation invocation = invoke(request(IMPORT_PATH, "", "wrong-key"), CONFIGURED_KEY);

        assertThat(invocation.response().getStatus()).isEqualTo(401);
        assertThat(invocation.chain().getRequest()).isNull();
    }
}
