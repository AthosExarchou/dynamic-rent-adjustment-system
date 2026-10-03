package gr.hua.dit.dras.config;

/* imports */
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    @Value("${app.public-url}")
    private String appPublicUrl;

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000", "http://localhost:8080", "http://localhost", appPublicUrl));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           LoginSuccessHandler successHandler) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .authorizeHttpRequests((requests) -> requests
                        /* Static Resources */
                        .requestMatchers("/images/**", "/js/**", "/css/**").permitAll()

                        /* Public Web Endpoints */
                        .requestMatchers(
                                "/", "/home",
                                "/listings", "/listings/local", "/listings/filter", "/listings/{id}",
                                "/contact/contactus", "/contact/send",
                                "/privacy", "/about", "/TermsOfService",
                                "/register", "/saveUser", "/error"
                        ).permitAll()

                        /* REST Auth Endpoints */
                        .requestMatchers("/auth/**").permitAll()

                        /* Actuator health/info */
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        /* Protected API Endpoints */
                        .requestMatchers("/external-import/**").hasAuthority("ADMIN")

                        /* Role-specific checks are handled by @Secured in the controllers */
                        .anyRequest().authenticated()
                )

                .csrf((csrf) -> {
                        CookieCsrfTokenRepository tokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
                        tokenRepository.setCookiePath("/");
                        tokenRepository.setCookieName("XSRF-TOKEN-DRAS");
                        csrf.csrfTokenRepository(tokenRepository)
                            .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                            .ignoringRequestMatchers("/auth/login", "/auth/logout", "/notifications/**", "/saveUser");
                })
                .addFilterAfter(new OncePerRequestFilter() {
                    @Override
                    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
                        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
                        if (csrfToken != null) {
                            csrfToken.getToken();
                        }
                        filterChain.doFilter(request, response);
                    }
                }, BasicAuthenticationFilter.class)

                .formLogin((form) -> form
                        .loginPage("/login")
                        .successHandler(successHandler)
                        .permitAll()
                )

                .logout((logout) -> logout
                        /* Clear session and redirect to home on logout */
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .permitAll()
                )

                .sessionManagement((session) -> session
                        .maximumSessions(-1)
                        .sessionRegistry(sessionRegistry())
                );

        return http.build();

    }

}
