package gr.hua.dit.dras.integration;

/* imports */
import gr.hua.dit.dras.entities.Role;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.repositories.RoleRepository;
import gr.hua.dit.dras.repositories.UserRepository;
import gr.hua.dit.dras.services.infrastructure.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private EmailService emailService;

    @Test
    @DisplayName("Complete Auth Journey: Register -> Login -> Session Access -> Role Check -> Logout")
    void completeAuthJourney() throws Exception {
        String email = "flowuser_" + System.currentTimeMillis() + "@example.com";
        String username = "flowuser_" + (System.currentTimeMillis() % 100000);
        String password = "StrongPassword123!@#";

        // Register a new user
        mockMvc.perform(post("/saveUser")
                        .with(csrf())
                        .param("username", username)
                        .param("email", email)
                        .param("password", password))
                .andExpect(status().isOk());

        // Login with registered credentials
        String loginPayload = String.format("""
                {
                    "email": "%s",
                    "password": "%s"
                }
                """, email, password);

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.username").value(username))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).as("Session must not be null after successful login").isNotNull();

        // Access /auth/me with session
        mockMvc.perform(get("/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.username").value(username));

        // Access Admin-only resource with USER role -> expect 403 Forbidden
        mockMvc.perform(get("/users").session(session))
                .andExpect(status().isForbidden());

        // Logout
        mockMvc.perform(post("/auth/logout").session(session))
                .andExpect(status().isOk());

        assertThat(session.isInvalid()).isTrue();

        // Access /auth/me after logout -> expect 401 Unauthorized
        mockMvc.perform(get("/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin Auth Journey: Admin Login -> Session Access -> Access Admin Endpoint Successfully")
    void adminAuthJourney_AccessAdminEndpoints() throws Exception {
        String email = "flowadmin_" + System.currentTimeMillis() + "@example.com";
        String username = "flowadmin_" + (System.currentTimeMillis() % 100000);
        String password = "AdminPassword123!@#";

        // Register base user
        mockMvc.perform(post("/saveUser")
                        .with(csrf())
                        .param("username", username)
                        .param("email", email)
                        .param("password", password))
                .andExpect(status().isOk());

        // Grant ADMIN role
        User adminUser = userRepository.findByEmail(email).orElseThrow();
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ADMIN")));
        adminUser.getRoles().add(adminRole);
        userRepository.saveAndFlush(adminUser);

        // Login as Admin
        String loginPayload = String.format("""
                {
                    "email": "%s",
                    "password": "%s"
                }
                """, email, password);

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andReturn();

        MockHttpSession adminSession = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(adminSession).isNotNull();

        // Access admin-only resource /users
        mockMvc.perform(get("/users").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Login with invalid credentials returns 401 Unauthorized")
    void login_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        String invalidPayload = """
                {
                    "email": "nonexistent@example.com",
                    "password": "WrongPassword123!"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }
}
