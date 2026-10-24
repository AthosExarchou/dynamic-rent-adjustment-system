package gr.hua.dit.dras.controllers.auth;

/* imports */
import gr.hua.dit.dras.config.LoginSuccessHandler;
import gr.hua.dit.dras.config.SecurityConfig;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RestAuthController.class)
@Import(SecurityConfig.class)
class RestAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private LoginSuccessHandler loginSuccessHandler;

    @Test
    @DisplayName("Login with valid credentials returns 200 OK and UserDTO")
    void login_ValidCredentials_ReturnsUserDto() throws Exception {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 10);
        user.setUsername("john_doe");
        user.setEmail("john@example.com");

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "john@example.com", "password123", Collections.emptyList());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        String loginJson = """
                {
                    "email": "john@example.com",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));

        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Login with invalid credentials returns 401 Unauthorized")
    void login_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        String loginJson = """
                {
                    "email": "wrong@example.com",
                    "password": "wrongPassword"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    @DisplayName("/auth/me without authentication returns 401 Unauthorized")
    void getCurrentUser_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/auth/me with authenticated user returns current user DTO")
    @WithMockUser(username = "jane@example.com")
    void getCurrentUser_Authenticated_ReturnsUserDto() throws Exception {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 20);
        user.setUsername("jane_doe");
        user.setEmail("jane@example.com");

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.username").value("jane_doe"))
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    @DisplayName("Logout invalidates session and returns 200 OK")
    void logout_ReturnsOk() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk());
    }
}
