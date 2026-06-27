package gr.hua.dit.dras.controllers.user;

import gr.hua.dit.dras.dto.AccountDeletionRequest;
import gr.hua.dit.dras.dto.UserEditRequest;
import gr.hua.dit.dras.entities.Role;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.repositories.RoleRepository;
import gr.hua.dit.dras.repositories.UserRepository;
import gr.hua.dit.dras.services.application.UserApplicationService;
import gr.hua.dit.dras.services.domain.UserService;
import gr.hua.dit.dras.services.infrastructure.EmailService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    private BindingResult bindingResult;
    @Mock
    private HttpSession session;

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

    @Test
    void saveUser_Success() {
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userService.saveUser(user)).thenReturn(1);

        ResponseEntity<?> response = userController.saveUser(user, bindingResult);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(emailService).sendWelcomeEmail("test@example.com", user);
    }

    @Test
    void saveUser_EmailAlreadyExists() {
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(new User()));

        ResponseEntity<?> response = userController.saveUser(user, bindingResult);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(userService, never()).saveUser(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void showUsers_Success() {
        List<User> users = new ArrayList<>();
        users.add(user);
        when(userService.getUsers()).thenReturn(users);

        List<gr.hua.dit.dras.dto.UserDTO> result = userController.showUsers();

        assertEquals(1, result.size());
        assertEquals(user.getUsername(), result.get(0).getUsername());
    }

    @Test
    void editUser_Success() {
        UserEditRequest request = new UserEditRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");

        when(userService.isUsernameTaken("newuser", 1)).thenReturn(false);
        when(userService.isEmailTaken("new@example.com", 1)).thenReturn(false);
        when(bindingResult.hasErrors()).thenReturn(false);

        org.springframework.security.core.context.SecurityContextHolder.setContext(
            mock(org.springframework.security.core.context.SecurityContext.class)
        );
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()).thenReturn(auth);
        doReturn(java.util.Collections.singletonList(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("USER"))).when(auth).getAuthorities();
        when(userService.getCurrentUserId()).thenReturn(1);

        ResponseEntity<?> response = userController.editUser(1, request, bindingResult, session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userApplicationService).editUser(1, request);
    }

    @Test
    void editUser_ValidationError() {
        UserEditRequest request = new UserEditRequest();
        when(userService.isUsernameTaken(request.getUsername(), 1)).thenReturn(true);
        when(bindingResult.hasErrors()).thenReturn(true);

        ResponseEntity<?> response = userController.editUser(1, request, bindingResult, session);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
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

        ResponseEntity<?> response = userController.deleteRolefromUser(1, 2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(user.getRoles().contains(role));
        verify(userService).updateUser(user);
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

        ResponseEntity<?> response = userController.addRoletoUser(1, 2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(user.getRoles().contains(role));
        verify(userService).updateUser(user);
    }

    @Test
    void addRoletoUser_Owner_WithoutOwnerProfile() {
        Role role = new Role();
        role.setId(3);
        role.setName("OWNER");

        when(userService.getUser(1)).thenReturn(user);
        when(roleRepository.findById(3)).thenReturn(Optional.of(role));

        ResponseEntity<?> response = userController.addRoletoUser(1, 3);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(userService, never()).updateUser(any());
    }

    @Test
    void deleteUser_Success() {
        ResponseEntity<?> response = userController.deleteUser(1);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userApplicationService).deleteUserAsAdmin(1);
    }

    @Test
    void deleteOwnAccount_Success() {
        AccountDeletionRequest request = new AccountDeletionRequest();
        when(bindingResult.hasErrors()).thenReturn(false);

        ResponseEntity<?> response = userController.deleteOwnAccount(request, bindingResult, session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userApplicationService).deleteCurrentUserAccount(request);
        verify(session).invalidate();
    }

    @Test
    void deleteOwnAccount_ValidationErrors() {
        AccountDeletionRequest request = new AccountDeletionRequest();
        when(bindingResult.hasErrors()).thenReturn(true);

        ResponseEntity<?> response = userController.deleteOwnAccount(request, bindingResult, session);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(userApplicationService, session);
    }
}
