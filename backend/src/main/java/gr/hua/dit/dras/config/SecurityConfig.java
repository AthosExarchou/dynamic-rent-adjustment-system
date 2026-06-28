package gr.hua.dit.dras.config;

/* imports */
import org.springframework.context.annotation.Bean;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
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
                        .requestMatchers("/api/auth/**").permitAll()

                        /* Protected API Endpoints */
                        .requestMatchers("/api/external-import/**").hasAuthority("ADMIN")

                        /* Role-specific checks are handled by @Secured in the controllers */
                        .anyRequest().authenticated()
                )

                /* BUG-B02 FIX: CSRF is now disabled only for the specific REST auth
                 * endpoints (/api/auth/login, /api/auth/logout) that are called by the SPA
                 * before a session is established. All other /api/** endpoints retain full
                 * CSRF protection, because this app uses session-based authentication. */
                .csrf((csrf) -> csrf
                        .ignoringRequestMatchers("/api/auth/login", "/api/auth/logout")
                )

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
