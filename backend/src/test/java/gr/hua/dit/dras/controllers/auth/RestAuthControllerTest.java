package gr.hua.dit.dras.controllers.auth;

/* imports */
import gr.hua.dit.dras.config.LoginSuccessHandler;
import gr.hua.dit.dras.config.SecurityConfig;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.entities.PasswordResetToken;
import gr.hua.dit.dras.entities.Role;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.repositories.PasswordResetTokenRepository;
import gr.hua.dit.dras.repositories.UserRepository;
import gr.hua.dit.dras.services.infrastructure.EmailService;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @MockitoBean
    private LoginSuccessHandler loginSuccessHandler;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private BCryptPasswordEncoder passwordEncoder;

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

    @Test
    @DisplayName("Forgot password with active account generates 6-digit code and dispatches email")
    void forgotPassword_ActiveAccount_GeneratesCodeAndSendsEmail() throws Exception {
        User user = new User();
        user.setUsername("active_user");
        user.setEmail("active@example.com");

        when(userRepository.findByEmail("active@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUser(user)).thenReturn(Optional.empty());

        String payload = """
                {
                    "email": "active@example.com"
                }
                """;

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("active@example.com"))
                .andExpect(jsonPath("$.message").value("A 6-digit confirmation code has been sent to your email address."));

        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetCodeEmail(eq("active@example.com"), eq(user), anyString());
    }

    @Test
    @DisplayName("Forgot password with non-existent email returns 404 Not Found")
    void forgotPassword_NonExistentEmail_ReturnsNotFound() throws Exception {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        String payload = """
                {
                    "email": "nonexistent@example.com"
                }
                """;

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No active account found with this email address."));
    }

    @Test
    @DisplayName("Forgot password with blank email returns 400 Bad Request")
    void forgotPassword_BlankEmail_ReturnsBadRequest() throws Exception {
        String payload = """
                {
                    "email": "   "
                }
                """;

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email is required."));
    }

    @Test
    @DisplayName("Reset password with valid code and matching new password updates password successfully")
    void resetPassword_ValidCodeAndNewPassword_Success() throws Exception {
        User user = new User();
        user.setUsername("reset_user");
        user.setEmail("user@example.com");
        user.setPassword("OldHashedPassword123!");

        PasswordResetToken token = new PasswordResetToken("123456", user, LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUser(user)).thenReturn(Optional.of(token));
        when(passwordEncoder.matches("NewStrongPass1!", "OldHashedPassword123!")).thenReturn(false);
        when(passwordEncoder.encode("NewStrongPass1!")).thenReturn("EncodedNewPassword456!");

        String payload = """
                {
                    "email": "user@example.com",
                    "code": "123456",
                    "newPassword": "NewStrongPass1!",
                    "confirmPassword": "NewStrongPass1!"
                }
                """;

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "Your password has been reset successfully. You can now sign in with your new password."));

        assertEquals("EncodedNewPassword456!", user.getPassword());
        assertNotNull(user.getPasswordChangedAt());
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).delete(token);
    }

    @Test
    @DisplayName("Reset password with invalid code returns 400 Bad Request")
    void resetPassword_InvalidCode_ReturnsBadRequest() throws Exception {
        User user = new User();
        user.setEmail("user@example.com");

        PasswordResetToken token = new PasswordResetToken("123456", user, LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUser(user)).thenReturn(Optional.of(token));

        String payload = """
                {
                    "email": "user@example.com",
                    "code": "999999",
                    "newPassword": "NewStrongPass1!",
                    "confirmPassword": "NewStrongPass1!"
                }
                """;

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid confirmation code."));
    }

    @Test
    @DisplayName("Reset password with expired code returns 400 Bad Request")
    void resetPassword_ExpiredCode_ReturnsBadRequest() throws Exception {
        User user = new User();
        user.setEmail("user@example.com");

        PasswordResetToken token = new PasswordResetToken("123456", user, LocalDateTime.now().minusMinutes(1)); // expired

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUser(user)).thenReturn(Optional.of(token));

        String payload = """
                {
                    "email": "user@example.com",
                    "code": "123456",
                    "newPassword": "NewStrongPass1!",
                    "confirmPassword": "NewStrongPass1!"
                }
                """;

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "Confirmation code has expired. Please request a new one."));
    }

    @Test
    @DisplayName("Reset password with mismatched passwords returns 400 Bad Request")
    void resetPassword_MismatchedPasswords_ReturnsBadRequest() throws Exception {
        User user = new User();
        user.setEmail("user@example.com");

        PasswordResetToken token = new PasswordResetToken("123456", user, LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUser(user)).thenReturn(Optional.of(token));

        String payload = """
                {
                    "email": "user@example.com",
                    "code": "123456",
                    "newPassword": "NewStrongPass1!",
                    "confirmPassword": "DifferentPassword2@"
                }
                """;

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("New password and confirmation do not match."));
    }

    @Test
    @DisplayName("Reset password with weak password returns 400 Bad Request")
    void resetPassword_WeakPassword_ReturnsBadRequest() throws Exception {
        User user = new User();
        user.setEmail("user@example.com");

        PasswordResetToken token = new PasswordResetToken("123456", user, LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUser(user)).thenReturn(Optional.of(token));

        String payload = """
                {
                    "email": "user@example.com",
                    "code": "123456",
                    "newPassword": "weak",
                    "confirmPassword": "weak"
                }
                """;

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "Password must contain 8+ characters, including uppercase, lowercase, a number, and a special character."));
    }

    @Test
    @DisplayName("Reset password allows restoring same password during recovery and succeeds with 200 OK")
    void resetPassword_SameAsOldPassword_SucceedsDuringRecovery() throws Exception {
        User user = new User();
        user.setEmail("user@example.com");
        user.setPassword("OldHashedPassword123!");

        PasswordResetToken token = new PasswordResetToken("123456", user, LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUser(user)).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("OldHashedPassword123!")).thenReturn("NewHashedPassword123!");

        String payload = """
                {
                    "email": "user@example.com",
                    "code": "123456",
                    "newPassword": "OldHashedPassword123!",
                    "confirmPassword": "OldHashedPassword123!"
                }
                """;

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "Your password has been reset successfully. You can now sign in with your new password."));

        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).delete(token);
    }

    @Test
    @DisplayName("Forgot password for ADMIN account returns 403 Forbidden")
    void forgotPassword_AdminAccount_ReturnsForbidden() throws Exception {
        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@gmail.com");
        adminUser.getRoles().add(new Role("ADMIN"));

        when(userRepository.findByEmail("admin@gmail.com")).thenReturn(Optional.of(adminUser));

        String payload = """
                {
                    "email": "admin@gmail.com"
                }
                """;

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value(
                        "Password recovery is disabled for administrator accounts. Please use server console access."));
    }

    @Test
    @DisplayName("Forgot password for system external user returns 404 Not Found")
    void forgotPassword_SystemAccount_ReturnsNotFound() throws Exception {
        User systemUser = new User();
        systemUser.setUsername("external-system");
        systemUser.setEmail("system@external.local");
        Owner owner = new Owner();
        owner.setSystemOwner(true);
        systemUser.setOwner(owner);

        when(userRepository.findByEmail("system@external.local")).thenReturn(Optional.of(systemUser));

        String payload = """
                {
                    "email": "system@external.local"
                }
                """;

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No active account found with this email address."));
    }

    @Test
    @DisplayName("Reset password for ADMIN account returns 403 Forbidden")
    void resetPassword_AdminAccount_ReturnsForbidden() throws Exception {
        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@gmail.com");
        adminUser.getRoles().add(new Role("ADMIN"));

        when(userRepository.findByEmail("admin@gmail.com")).thenReturn(Optional.of(adminUser));

        String payload = """
                {
                    "email": "admin@gmail.com",
                    "code": "123456",
                    "newPassword": "NewStrongPass1!",
                    "confirmPassword": "NewStrongPass1!"
                }
                """;

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value(
                        "Password recovery is disabled for administrator accounts. Please use server console access."));
    }

    @Test
    @DisplayName("Reset password for system external user returns 404 Not Found")
    void resetPassword_SystemAccount_ReturnsNotFound() throws Exception {
        User systemUser = new User();
        systemUser.setUsername("external-system");
        systemUser.setEmail("system@external.local");
        Owner owner = new Owner();
        owner.setSystemOwner(true);
        systemUser.setOwner(owner);

        when(userRepository.findByEmail("system@external.local")).thenReturn(Optional.of(systemUser));

        String payload = """
                {
                    "email": "system@external.local",
                    "code": "123456",
                    "newPassword": "NewStrongPass1!",
                    "confirmPassword": "NewStrongPass1!"
                }
                """;

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No active account found with this email address."));
    }
}
