package gr.hua.dit.dras.controllers.user;

/* imports */
import gr.hua.dit.dras.dto.AccountDeletionRequest;
import gr.hua.dit.dras.dto.UserEditRequest;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.entities.Role;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.repositories.RoleRepository;
import gr.hua.dit.dras.repositories.UserRepository;
import gr.hua.dit.dras.services.application.UserApplicationService;
import gr.hua.dit.dras.services.domain.UserService;
import gr.hua.dit.dras.services.infrastructure.EmailService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private UserApplicationService userApplicationService;

    @Mock
    private Model model;
    @Mock
    private BindingResult bindingResult;
    @Mock
    private RedirectAttributes redirectAttributes;
    @Mock
    private HttpSession session;
    @Mock
    private Authentication authentication;
    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private UserController userController;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addCommonAttributes_AuthenticatedUser() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("test@example.com");
        doReturn(Collections.singletonList(
                new SimpleGrantedAuthority("USER"))).when(authentication).getAuthorities();
        when(userService.getUserByEmail("test@example.com")).thenReturn(user);

        userController.addCommonAttributes(model);

        verify(model).addAttribute("currentUserId", 1);
        verify(model).addAttribute("currentUserIsAdmin", false);
    }

    @Test
    void addCommonAttributes_AnonymousUser() {
        SecurityContextHolder.setContext(securityContext);
        AnonymousAuthenticationToken anonymousToken = mock(AnonymousAuthenticationToken.class);
        when(securityContext.getAuthentication()).thenReturn(anonymousToken);
        when(anonymousToken.isAuthenticated()).thenReturn(true);

        userController.addCommonAttributes(model);

        verify(model).addAttribute("currentUserId", null);
        verify(model).addAttribute("currentUserIsAdmin", false);
    }

    @Test
    void register_AddsUserToModel() {
        when(model.containsAttribute("user")).thenReturn(false);
        String viewName = userController.register(model);
        assertEquals("auth/register", viewName);
        verify(model).addAttribute(eq("user"), any(User.class));
    }

    @Test
    void saveUser_Success() {
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userService.saveUser(user)).thenReturn(1);

        String viewName = userController.saveUser(user, bindingResult, redirectAttributes);

        assertEquals("redirect:/login", viewName);
        verify(emailService).sendWelcomeEmail("test@example.com", user);
        verify(redirectAttributes).addFlashAttribute(
                "successMessage", "User '1' saved successfully !");
    }

    @Test
    void saveUser_EmailAlreadyExists() {
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(new User()));
        when(bindingResult.hasErrors()).thenReturn(true);

        String viewName = userController.saveUser(user, bindingResult, redirectAttributes);

        assertEquals("redirect:/register", viewName);
        verify(bindingResult).rejectValue(
                "email", "error.user", "Email already registered!");
        verify(redirectAttributes).addFlashAttribute(
                "org.springframework.validation.BindingResult.user", bindingResult);
        verify(redirectAttributes).addFlashAttribute("user", user);
        verify(userService, never()).saveUser(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void showUsers_Success() {
        List<User> users = new ArrayList<>();
        users.add(user);
        when(userService.getUsers()).thenReturn(users);
        when(roleRepository.findAll()).thenReturn(new ArrayList<>());
        when(userService.getCurrentUserId()).thenReturn(1);

        String viewName = userController.showUsers(model);

        assertEquals("auth/users", viewName);
        verify(model).addAttribute("users", users);
        verify(model).addAttribute("roles", new ArrayList<>());
        verify(model).addAttribute("currentUserId", 1);
    }

    @Test
    void editUser_Success_SelfEdit() {
        UserEditRequest request = new UserEditRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");

        when(userService.isUsernameTaken("newuser", 1)).thenReturn(false);
        when(userService.isEmailTaken("new@example.com", 1)).thenReturn(false);
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userApplicationService.editUser(1, request)).thenReturn(true);

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Collections.singletonList(
                new SimpleGrantedAuthority("USER"))).when(authentication).getAuthorities();
        when(userService.getCurrentUserId()).thenReturn(1);

        String viewName = userController.editUser(1, request, bindingResult, redirectAttributes, session);

        assertEquals("redirect:/login", viewName);
        verify(redirectAttributes).addFlashAttribute(
                "successMessage", "Profile updated successfully.");
        verify(redirectAttributes).addFlashAttribute(
                "infoMessage", "Profile updated. Please log in again.");
        verify(userApplicationService).editUser(1, request);
        verify(session).invalidate();
    }

    @Test
    void editUser_ValidationError() {
        UserEditRequest request = new UserEditRequest();
        when(userService.isUsernameTaken(request.getUsername(), 1)).thenReturn(true);
        when(bindingResult.hasErrors()).thenReturn(true);

        String viewName = userController.editUser(1, request, bindingResult, redirectAttributes, session);

        assertEquals("redirect:/user/1", viewName);
        verify(bindingResult).rejectValue(
                "username", "error.userEditRequest", "This username is already taken.");
        verify(redirectAttributes).addFlashAttribute(
                "org.springframework.validation.BindingResult.userEditRequest", bindingResult);
        verify(redirectAttributes).addFlashAttribute("userEditRequest", request);
        verify(redirectAttributes).addFlashAttribute(
                "errorMessage", "Please correct the highlighted errors.");
        verify(userApplicationService, never()).editUser(anyInt(), any());
    }

    @Test
    void deleteRolefromUser_Success() {
        Role role = new Role();
        role.setId(2);
        role.setName("USER");
        Set<Role> roles = new java.util.HashSet<>();
        roles.add(role);
        user.setRoles(roles);

        when(userService.getUser(1)).thenReturn(user);
        when(roleRepository.findById(2)).thenReturn(Optional.of(role));

        String viewName = userController.deleteRolefromUser(1, 2, redirectAttributes);

        assertEquals("redirect:/users", viewName);
        assertFalse(user.getRoles().contains(role));
        verify(userService).updateUser(user);
        verify(redirectAttributes).addFlashAttribute(
                "successMessage", "Role removed successfully.");
    }

    @Test
    void addRoletoUser_UserRole_Success() {
        Role role = new Role();
        role.setId(2);
        role.setName("USER");

        when(userService.getUser(1)).thenReturn(user);
        when(roleRepository.findById(2)).thenReturn(Optional.of(role));
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        user.setRoles(new java.util.HashSet<>());

        String viewName = userController.addRoletoUser(1, 2, model, redirectAttributes);

        assertEquals("redirect:/users", viewName);
        assertTrue(user.getRoles().contains(role));
        verify(userService).updateUser(user);
        verify(redirectAttributes).addFlashAttribute(
                "successMessage", "User role added successfully.");
    }

    @Test
    void addRoletoUser_Owner_WithoutOwnerProfile() {
        Role role = new Role();
        role.setId(3);
        role.setName("OWNER");

        when(userService.getUser(1)).thenReturn(user);
        when(roleRepository.findById(3)).thenReturn(Optional.of(role));

        String viewName = userController.addRoletoUser(1, 3, model, redirectAttributes);

        assertEquals("owner/ownerform", viewName);
        verify(model).addAttribute(eq("owner"), any(Owner.class));
        verify(model).addAttribute("userId", 1);
        verify(userService, never()).updateUser(any());
    }

    @Test
    void deleteUser_Success() {
        String viewName = userController.deleteUser(1, redirectAttributes);
        assertEquals("redirect:/users", viewName);
        verify(userApplicationService).deleteUserAsAdmin(1);
        verify(redirectAttributes).addFlashAttribute(
                "successMessage", "User deleted successfully.");
    }

    @Test
    void deleteOwnAccount_Success() {
        AccountDeletionRequest request = new AccountDeletionRequest();
        when(bindingResult.hasErrors()).thenReturn(false);

        String viewName = userController.deleteOwnAccount(request, bindingResult, redirectAttributes, session);

        assertEquals("redirect:/", viewName);
        verify(userApplicationService).deleteCurrentUserAccount(request);
        verify(session).invalidate();
    }

    @Test
    void deleteOwnAccount_ValidationErrors() {
        AccountDeletionRequest request = new AccountDeletionRequest();
        when(bindingResult.hasErrors()).thenReturn(true);

        String viewName = userController.deleteOwnAccount(request, bindingResult, redirectAttributes, session);

        assertEquals("redirect:/user/delete/self", viewName);
        verify(redirectAttributes).addFlashAttribute(
                "org.springframework.validation.BindingResult.deletionRequest", bindingResult);
        verify(redirectAttributes).addFlashAttribute("deletionRequest", request);
        verify(redirectAttributes).addFlashAttribute(
                "errorMessage", "Please fill out all required fields.");
        verifyNoInteractions(userApplicationService, session);
    }
}
