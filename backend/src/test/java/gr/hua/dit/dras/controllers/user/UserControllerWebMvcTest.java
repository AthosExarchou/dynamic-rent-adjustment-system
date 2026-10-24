package gr.hua.dit.dras.controllers.user;

import gr.hua.dit.dras.config.LoginSuccessHandler;
import gr.hua.dit.dras.config.SecurityConfig;
import gr.hua.dit.dras.entities.Role;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.repositories.RoleRepository;
import gr.hua.dit.dras.repositories.UserRepository;
import gr.hua.dit.dras.services.application.UserApplicationService;
import gr.hua.dit.dras.services.domain.UserService;
import gr.hua.dit.dras.services.infrastructure.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean(name = "userService")
    private UserService userService;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private UserApplicationService userApplicationService;

    @MockitoBean
    private SessionRegistry sessionRegistry;

    @MockitoBean
    private LoginSuccessHandler loginSuccessHandler;

    @Test
    @DisplayName("saveUser with invalid email triggers Spring Bean Validation and returns 400")
    void saveUser_InvalidEmail_TriggersBeanValidation_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/saveUser")
                        .with(csrf())
                        .param("username", "validUser")
                        .param("email", "not-a-valid-email")
                        .param("password", "ValidPass123!@#"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("saveUser with blank username triggers Spring Bean Validation and returns 400")
    void saveUser_BlankUsername_TriggersBeanValidation_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/saveUser")
                        .with(csrf())
                        .param("username", "")
                        .param("email", "valid@example.com")
                        .param("password", "ValidPass123!@#"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("saveUser with weak password returns 400 Bad Request")
    void saveUser_WeakPassword_ReturnsBadRequest() throws Exception {
        when(userRepository.findByUsername("validUser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("valid@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/saveUser")
                        .with(csrf())
                        .param("username", "validUser")
                        .param("email", "valid@example.com")
                        .param("password", "weak"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Password must contain 8+ characters, including uppercase, lowercase, a number, and a special character."));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("saveUser with duplicate username returns 400 Bad Request")
    void saveUser_DuplicateUsername_ReturnsBadRequest() throws Exception {
        when(userRepository.findByUsername("existingUser")).thenReturn(Optional.of(new User()));

        mockMvc.perform(post("/saveUser")
                        .with(csrf())
                        .param("username", "existingUser")
                        .param("email", "valid@example.com")
                        .param("password", "ValidPass123!@#"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username already taken!"));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("saveUser with duplicate email returns 400 Bad Request")
    void saveUser_DuplicateEmail_ReturnsBadRequest() throws Exception {
        when(userRepository.findByUsername("uniqueUser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(new User()));

        mockMvc.perform(post("/saveUser")
                        .with(csrf())
                        .param("username", "uniqueUser")
                        .param("email", "existing@example.com")
                        .param("password", "ValidPass123!@#"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already registered!"));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("saveUser with valid payload returns 200 OK and persists user")
    void saveUser_ValidPayload_ReturnsOk() throws Exception {
        when(userRepository.findByUsername("newUser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userService.saveUser(any(User.class))).thenReturn(101);

        mockMvc.perform(post("/saveUser")
                        .with(csrf())
                        .param("username", "newUser")
                        .param("email", "new@example.com")
                        .param("password", "ValidPass123!@#"))
                .andExpect(status().isOk());

        verify(userService).saveUser(any(User.class));
        verify(emailService).sendWelcomeEmail(any(), any());
    }

    @Test
    @DisplayName("showUser self access returns 200 OK and UserDTO")
    @WithMockUser(username = "self@test.com", authorities = "USER")
    void showUser_Self_ReturnsUserDto() throws Exception {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 5);
        user.setUsername("selfUser");
        user.setEmail("self@test.com");

        when(userService.getCurrentUserId()).thenReturn(5);
        when(userService.getUser(5)).thenReturn(user);

        mockMvc.perform(get("/user/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.username").value("selfUser"));
    }

    @Test
    @DisplayName("showUser by admin returns 200 OK and UserDTO")
    @WithMockUser(username = "admin@test.com", authorities = "ADMIN")
    void showUser_Admin_ReturnsUserDto() throws Exception {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 5);
        user.setUsername("targetUser");
        user.setEmail("target@test.com");

        when(userService.getUser(5)).thenReturn(user);

        mockMvc.perform(get("/user/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.username").value("targetUser"));
    }

    @Test
    @DisplayName("showUser by unauthorized other user returns 403 Forbidden")
    @WithMockUser(username = "other@test.com", authorities = "USER")
    void showUser_OtherUser_ReturnsForbidden() throws Exception {
        when(userService.getCurrentUserId()).thenReturn(99);

        mockMvc.perform(get("/user/5"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("editUser self access returns 200 OK")
    @WithMockUser(username = "self@test.com", authorities = "USER")
    void editUser_Self_ReturnsOk() throws Exception {
        when(userService.getCurrentUserId()).thenReturn(7);
        when(userService.isUsernameTaken("updatedName", 7)).thenReturn(false);
        when(userService.isEmailTaken("updated@test.com", 7)).thenReturn(false);

        mockMvc.perform(post("/user/7")
                        .with(csrf())
                        .param("username", "updatedName")
                        .param("email", "updated@test.com"))
                .andExpect(status().isOk());

        verify(userApplicationService).editUser(any(), any());
    }

    @Test
    @DisplayName("deleteRolefromUser by admin returns 200 OK")
    @WithMockUser(authorities = "ADMIN")
    void deleteRolefromUser_Admin_ReturnsOk() throws Exception {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 10);
        Role role = new Role("OWNER");
        ReflectionTestUtils.setField(role, "id", 2);
        user.setRoles(new java.util.HashSet<>(Set.of(role)));

        when(userService.getUser(10)).thenReturn(user);
        when(roleRepository.findById(2)).thenReturn(Optional.of(role));

        mockMvc.perform(post("/user/role/delete/10/2")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(userService).updateUser(user);
    }

    @Test
    @DisplayName("deleteRolefromUser by non-admin returns 403 Forbidden")
    @WithMockUser(authorities = "USER")
    void deleteRolefromUser_NonAdmin_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/user/role/delete/10/2")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("deleteUser by admin returns 200 OK")
    @WithMockUser(authorities = "ADMIN")
    void deleteUser_Admin_ReturnsOk() throws Exception {
        mockMvc.perform(post("/user/delete/15")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(userApplicationService).deleteUserAsAdmin(15);
    }

    @Test
    @DisplayName("deleteUser by non-admin returns 403 Forbidden")
    @WithMockUser(authorities = "USER")
    void deleteUser_NonAdmin_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/user/delete/15")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userApplicationService);
    }

    @Test
    @DisplayName("deleteOwnAccount with blank confirmation phrase triggers Bean Validation")
    @WithMockUser(authorities = "USER")
    void deleteOwnAccount_BlankFields_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/user/delete/self")
                        .with(csrf())
                        .param("confirmationPhrase", "")
                        .param("password", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Please fill out all required fields."));

        verifyNoInteractions(userApplicationService);
    }

    @Test
    @DisplayName("deleteOwnAccount without CSRF returns 403 Forbidden")
    @WithMockUser(authorities = "USER")
    void deleteOwnAccount_WithoutCsrf_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/user/delete/self")
                        .param("confirmationPhrase", "DELETE MY ACCOUNT")
                        .param("password", "Pass123!"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userApplicationService);
    }
}
