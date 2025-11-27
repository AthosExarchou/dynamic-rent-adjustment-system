package gr.hua.dit.dras.config;

/* imports */
import gr.hua.dit.dras.services.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    private UserService userService;
    private UserDetailsService userDetailsService;
    private BCryptPasswordEncoder passwordEncoder;

    public SecurityConfig(UserService userService, UserDetailsService userDetailsService, BCryptPasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           LoginSuccessHandler successHandler) throws Exception {
        http
                .authorizeHttpRequests((requests) -> requests
                        /* specific restrictions */
                        .requestMatchers("/listing/new", "/listing/assign/**").hasRole("USER")
                        .requestMatchers("/users").hasRole("ADMIN")
                        .requestMatchers("/listing/delete/**").hasRole("OWNER")
                        /* public endpoints */
                        .requestMatchers("/", "/home", "/listing", "/api/external-import/**", "/contact/contactus", "/privacy", "/about", "/TermsOfService", "/register", "/saveUser", "/images/**", "/js/**", "/css/**").permitAll()
                        /* any other request */
                        .anyRequest().authenticated()
                )

                .formLogin((form) -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/listing", true)
                        .successHandler(successHandler)
                        .permitAll())
                .logout((logout) -> logout.permitAll());

        return http.build();

    }

}
