package gr.hua.dit.dras.config;

/* imports */
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           LoginSuccessHandler successHandler) throws Exception {
        http
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

                        /* Public API Endpoints */
                        .requestMatchers("/api/external-import/**").hasAuthority("ADMIN")

                        /* Role-specific checks are handled by @Secured in the controllers */
                        .anyRequest().authenticated()
                )

                /* Disable CSRF only for the secured REST API */
                .csrf((csrf) -> csrf
                        .ignoringRequestMatchers("/api/external-import/**")
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
                );

        return http.build();

    }

}
