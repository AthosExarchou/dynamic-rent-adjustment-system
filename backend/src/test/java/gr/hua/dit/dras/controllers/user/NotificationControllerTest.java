package gr.hua.dit.dras.controllers.user;

/* imports */
import gr.hua.dit.dras.config.LoginSuccessHandler;
import gr.hua.dit.dras.config.SecurityConfig;
import gr.hua.dit.dras.dto.NotificationDTO;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.repositories.UserRepository;
import gr.hua.dit.dras.services.domain.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private LoginSuccessHandler loginSuccessHandler;

    @Test
    @DisplayName("Unauthenticated request to /notifications is redirected to login")
    void getNotifications_Unauthenticated_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Authenticated user fetches notifications")
    @WithMockUser(username = "user@test.com")
    void getNotifications_Authenticated_ReturnsList() throws Exception {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 42);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        NotificationDTO dto = new NotificationDTO();
        dto.setId(1);
        dto.setMessage("New application received");
        when(notificationService.getUserNotifications(42)).thenReturn(List.of(dto));

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].message").value("New application received"));

        verify(notificationService).getUserNotifications(42);
    }

    @Test
    @DisplayName("Authenticated user not found in database returns 401 Unauthorized")
    @WithMockUser(username = "missing@test.com")
    void getNotifications_UserNotFoundInDb_ReturnsUnauthorized() throws Exception {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Authenticated user gets unread notification count")
    @WithMockUser(username = "user@test.com")
    void getUnreadCount_Authenticated_ReturnsCount() throws Exception {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 42);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(notificationService.getUnreadCount(42)).thenReturn(3L);

        mockMvc.perform(get("/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));

        verify(notificationService).getUnreadCount(42);
    }

    @Test
    @DisplayName("Authenticated user marks notification as read")
    @WithMockUser(username = "user@test.com")
    void markAsRead_Authenticated_ReturnsOk() throws Exception {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 42);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        mockMvc.perform(put("/notifications/5/read"))
                .andExpect(status().isOk());

        verify(notificationService).markAsRead(5, 42);
    }

    @Test
    @DisplayName("Authenticated user marks all notifications as read")
    @WithMockUser(username = "user@test.com")
    void markAllAsRead_Authenticated_ReturnsOk() throws Exception {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 42);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        mockMvc.perform(put("/notifications/read-all"))
                .andExpect(status().isOk());

        verify(notificationService).markAllAsRead(42);
    }
}
