package gr.hua.dit.dras.config;

/* imports */
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Authenticates machine-to-machine calls to the external import API using a
 * shared secret header ({@code X-Scraper-Key}).
 *
 * <p>Browser sessions and {@code @WithMockUser} test contexts are left
 * untouched: when a non-anonymous authentication already exists, the request
 * is forwarded as-is. Anonymous requests without the header are also forwarded
 * so that Spring Security's authorization rules produce the usual 403.</p>
 */
public class ScraperApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Scraper-Key";

    private static final String PROTECTED_PATH_PREFIX = "/external-import/";
    private static final String AUTHORITY = "ADMIN";
    private static final String PRINCIPAL = "scraper-service";

    private final String expectedApiKey;

    public ScraperApiKeyFilter(String expectedApiKey) {
        this.expectedApiKey = expectedApiKey == null ? "" : expectedApiKey;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !path.startsWith(PROTECTED_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing != null && existing.isAuthenticated()
                && !(existing instanceof AnonymousAuthenticationToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader(HEADER_NAME);

        /* No key: fall through and let the authorization rules reject the request */
        if (providedKey == null || providedKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        /* Fail closed when the server key is missing or the values differ */
        if (expectedApiKey.isBlank() || !MessageDigest.isEqual(
                expectedApiKey.getBytes(StandardCharsets.UTF_8),
                providedKey.getBytes(StandardCharsets.UTF_8)
        )) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Invalid or unconfigured X-Scraper-Key\"}");
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        PRINCIPAL,
                        null,
                        List.of(new SimpleGrantedAuthority(AUTHORITY))
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
