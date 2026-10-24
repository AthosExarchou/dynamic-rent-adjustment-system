package gr.hua.dit.dras.controllers.user;

/* imports */
import gr.hua.dit.dras.config.AppConfig;
import gr.hua.dit.dras.config.LoginSuccessHandler;
import gr.hua.dit.dras.config.SecurityConfig;
import gr.hua.dit.dras.entities.Role;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.services.domain.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileController.class)
@Import({SecurityConfig.class, AppConfig.class})
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private LoginSuccessHandler loginSuccessHandler;

    @Test
    @DisplayName("Unauthenticated request to change-password redirects to login")
    void changePassword_Unauthenticated_RedirectsToLogin() throws Exception {
        mockMvc.perform(post("/user/change-password/1")
                        .with(csrf())
                        .param("oldPassword", "OldPass1!")
                        .param("newPassword", "NewPass1!")
                        .param("confirmPassword", "NewPass1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @DisplayName("User attempting to change another user's password returns 403 Forbidden")
    @WithMockUser(username = "attacker@test.com", authorities = "USER")
    void changePassword_OtherUserProfile_ReturnsForbidden() throws Exception {
        User attacker = new User();
        ReflectionTestUtils.setField(attacker, "id", 99);
        attacker.setEmail("attacker@test.com");
        attacker.setRoles(Set.of(new Role("USER")));

        when(userService.getUserByEmail("attacker@test.com")).thenReturn(attacker);

        mockMvc.perform(post("/user/change-password/1")
                        .with(csrf())
                        .param("oldPassword", "OldPass1!")
                        .param("newPassword", "NewPass1!")
                        .param("confirmPassword", "NewPass1!"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Incorrect old password returns 400 Bad Request")
    @WithMockUser(username = "user@test.com", authorities = "USER")
    void changePassword_WrongOldPassword_ReturnsBadRequest() throws Exception {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 1);
        user.setEmail("user@test.com");
        user.setPassword(passwordEncoder.encode("RealCurrentPassword1!"));
        user.setRoles(Set.of(new Role("USER")));

        when(userService.getUserByEmail("user@test.com")).thenReturn(user);
        when(userService.getUser(1)).thenReturn(user);

        mockMvc.perform(post("/user/change-password/1")
                        .with(csrf())
                        .param("oldPassword", "WrongPassword1!")
                        .param("newPassword", "NewValidPassword1!")
                        .param("confirmPassword", "NewValidPassword1!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Old password is incorrect."));
    }

    @Test
    @DisplayName("New password not matching confirmation returns 400 Bad Request")
    @WithMockUser(username = "user@test.com", authorities = "USER")
    void changePassword_MismatchConfirm_ReturnsBadRequest() throws Exception {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 1);
        user.setEmail("user@test.com");
        user.setPassword(passwordEncoder.encode("RealCurrentPassword1!"));
        user.setRoles(Set.of(new Role("USER")));

        when(userService.getUserByEmail("user@test.com")).thenReturn(user);
        when(userService.getUser(1)).thenReturn(user);

        mockMvc.perform(post("/user/change-password/1")
                        .with(csrf())
                        .param("oldPassword", "RealCurrentPassword1!")
                        .param("newPassword", "NewValidPassword1!")
                        .param("confirmPassword", "DifferentPassword1!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("New password and confirmation do not match."));
    }

    @Test
    @DisplayName("Weak new password returns 400 Bad Request with complexity error")
    @WithMockUser(username = "user@test.com", authorities = "USER")
    void changePassword_WeakNewPassword_ReturnsBadRequest() throws Exception {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 1);
        user.setEmail("user@test.com");
        user.setPassword(passwordEncoder.encode("RealCurrentPassword1!"));
        user.setRoles(Set.of(new Role("USER")));

        when(userService.getUserByEmail("user@test.com")).thenReturn(user);
        when(userService.getUser(1)).thenReturn(user);

        mockMvc.perform(post("/user/change-password/1")
                        .with(csrf())
                        .param("oldPassword", "RealCurrentPassword1!")
                        .param("newPassword", "simple")
                        .param("confirmPassword", "simple"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "Password must contain 8+ characters, including uppercase, lowercase, a number, and a special character."));
    }

    @Test
    @DisplayName("New password same as current password returns 400 Bad Request")
    @WithMockUser(username = "user@test.com", authorities = "USER")
    void changePassword_SameAsOldPassword_ReturnsBadRequest() throws Exception {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 1);
        user.setEmail("user@test.com");
        user.setPassword(passwordEncoder.encode("SamePassword123!"));
        user.setRoles(Set.of(new Role("USER")));

        when(userService.getUserByEmail("user@test.com")).thenReturn(user);
        when(userService.getUser(1)).thenReturn(user);

        mockMvc.perform(post("/user/change-password/1")
                        .with(csrf())
                        .param("oldPassword", "SamePassword123!")
                        .param("newPassword", "SamePassword123!")
                        .param("confirmPassword", "SamePassword123!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("New password cannot be the same as the old one."));
    }

    @Test
    @DisplayName("Request without CSRF returns 403 Forbidden")
    @WithMockUser(username = "user@test.com", authorities = "USER")
    void changePassword_WithoutCsrf_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/user/change-password/1")
                        .param("oldPassword", "OldPass1!")
                        .param("newPassword", "NewPass1!")
                        .param("confirmPassword", "NewPass1!"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Valid password change returns 200 OK and updates user with encoded password")
    @WithMockUser(username = "user@test.com", authorities = "USER")
    void changePassword_Success_ReturnsOk() throws Exception {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 1);
        user.setEmail("user@test.com");
        user.setPassword(passwordEncoder.encode("RealCurrentPassword1!"));
        user.setRoles(Set.of(new Role("USER")));

        when(userService.getUserByEmail("user@test.com")).thenReturn(user);
        when(userService.getUser(1)).thenReturn(user);

        mockMvc.perform(post("/user/change-password/1")
                        .with(csrf())
                        .param("oldPassword", "RealCurrentPassword1!")
                        .param("newPassword", "BrandNewPassword123!")
                        .param("confirmPassword", "BrandNewPassword123!"))
                .andExpect(status().isOk());

        verify(userService).updateUser(user);
        assertThat(passwordEncoder.matches("BrandNewPassword123!", user.getPassword())).isTrue();
        assertThat(user.getPasswordChangedAt()).isNotNull();
    }

    @Test
    @DisplayName("Admin modifying another user password returns 200 OK")
    @WithMockUser(username = "admin@test.com", authorities = "ADMIN")
    void changePassword_AdminUser_ReturnsOk() throws Exception {
        User admin = new User();
        ReflectionTestUtils.setField(admin, "id", 100);
        admin.setEmail("admin@test.com");
        admin.setRoles(Set.of(new Role("ADMIN")));

        User targetUser = new User();
        ReflectionTestUtils.setField(targetUser, "id", 1);
        targetUser.setEmail("user@test.com");
        targetUser.setPassword(passwordEncoder.encode("OldUserPassword123!"));
        targetUser.setRoles(Set.of(new Role("USER")));

        when(userService.getUserByEmail("admin@test.com")).thenReturn(admin);
        when(userService.getUser(1)).thenReturn(targetUser);

        mockMvc.perform(post("/user/change-password/1")
                        .with(csrf())
                        .param("oldPassword", "OldUserPassword123!")
                        .param("newPassword", "BrandNewPassword123!")
                        .param("confirmPassword", "BrandNewPassword123!"))
                .andExpect(status().isOk());

        verify(userService).updateUser(targetUser);
        assertThat(passwordEncoder.matches("BrandNewPassword123!", targetUser.getPassword())).isTrue();
    }
}
