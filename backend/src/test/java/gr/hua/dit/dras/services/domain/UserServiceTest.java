package gr.hua.dit.dras.services.domain;

/* imports */
import gr.hua.dit.dras.entities.Role;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.repositories.RoleRepository;
import gr.hua.dit.dras.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;
    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setEmail("test@test.com");
        testUser.setUsername("testuser");
        testUser.setPassword("rawpassword");

        userRole = new Role("USER");
        testUser.setRoles(Set.of(userRole));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should successfully encode password and save User with default USER role")
    void shouldSaveUserAndEncodePassword() {
        Role previousRole = new Role("ADMIN");
        testUser.setRoles(Set.of(previousRole));
        when(passwordEncoder.encode("rawpassword")).thenReturn("encoded");
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        Integer id = userService.saveUser(testUser);

        assertThat(testUser.getPassword()).isEqualTo("encoded");
        assertThat(testUser.getRoles()).containsExactly(userRole);
        assertThat(id).isEqualTo(1);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should fail save when default USER role is missing")
    void shouldThrowWhenDefaultUserRoleMissingOnSave() {
        when(passwordEncoder.encode("rawpassword")).thenReturn("encoded");
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.saveUser(testUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("USER role not found");

        assertThat(testUser.getPassword()).isEqualTo("encoded");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update existing user and return ID")
    void shouldUpdateUser() {
        when(userRepository.save(testUser)).thenReturn(testUser);

        Integer id = userService.updateUser(testUser);

        assertThat(id).isEqualTo(1);
        verify(userRepository).save(testUser);
        verifyNoInteractions(roleRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Should load UserDetails by username (email)")
    void shouldLoadUserByUsername() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userService.loadUserByUsername("test@test.com");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("test@test.com");
        assertThat(userDetails.getPassword()).isEqualTo("rawpassword");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("USER");
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException if user email not found")
    void shouldThrowWhenUsernameNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername("notfound@test.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Should get current User ID from SecurityContext")
    void shouldGetCurrentUserId() {
        authenticateAs("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));

        Integer id = userService.getCurrentUserId();

        assertThat(id).isEqualTo(1);
    }

    @Test
    @DisplayName("Should throw if authenticated principal is missing in repository")
    void shouldThrowIfAuthenticatedUserMissingWhenGettingCurrentUserId() {
        authenticateAs("missing@test.com");
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUserId())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User not found: missing@test.com");
    }

    @Test
    @DisplayName("Should throw if unauthenticated when getting current User ID")
    void shouldThrowIfUnauthenticatedWhenGettingCurrentUserId() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        assertThatThrownBy(() -> userService.getCurrentUserId())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not authenticated");
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Should check if User is Admin and throw if trying to modify")
    void shouldAssertNotAdmin() {
        Role adminRole = new Role("ADMIN");
        testUser.setRoles(Set.of(adminRole));

        assertThatThrownBy(() -> userService.assertNotAdmin(testUser))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Administrator account cannot be modified");
    }

    @Test
    @DisplayName("Should return true if current user owns listings")
    void shouldReturnTrueWhenCurrentUserIsOwner() {
        testUser.setRoles(Set.of(new Role("OWNER")));
        authenticateAs("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        boolean owner = userService.isUserOwner();

        assertThat(owner).isTrue();
    }

    @Test
    @DisplayName("Should return false if current user is not an owner")
    void shouldReturnFalseWhenCurrentUserIsNotOwner() {
        authenticateAs("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        boolean owner = userService.isUserOwner();

        assertThat(owner).isFalse();
    }

    @Test
    @DisplayName("Should check if username is taken by another user")
    void shouldCheckIfUsernameTaken() {
        User otherUser = new User();
        otherUser.setId(2);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(otherUser));

        boolean taken = userService.isUsernameTaken("testuser", 1);
        
        assertThat(taken).isTrue();
    }

    @Test
    @DisplayName("Should return false if username belongs to the same user")
    void shouldReturnFalseIfUsernameBelongsToSameUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        boolean taken = userService.isUsernameTaken("testuser", 1);
        
        assertThat(taken).isFalse();
    }

    @Test
    @DisplayName("Should check if email is taken by another user")
    void shouldCheckIfEmailTakenByAnotherUser() {
        User otherUser = new User();
        otherUser.setId(2);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(otherUser));

        boolean taken = userService.isEmailTaken("test@test.com", 1);
        
        assertThat(taken).isTrue();
    }

    @Test
    @DisplayName("Should return false if email belongs to the same user")
    void shouldReturnFalseIfEmailBelongsToSameUser() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));

        boolean taken = userService.isEmailTaken("test@test.com", 1);
        
        assertThat(taken).isFalse();
    }

    @Test
    @DisplayName("Should successfully delete user")
    void shouldDeleteUser() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        userService.deleteUser(1);

        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("Should throw when deleting non-existent user")
    void shouldThrowWhenDeletingNonExistentUser() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(999))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User not found");
        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should return list of all users")
    void shouldGetAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        List<User> users = userService.getUsers();

        assertThat(users).hasSize(1);
        assertThat(users).contains(testUser);
    }

    @Test
    @DisplayName("Should get user by ID")
    void shouldGetUserById() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        User user = userService.getUser(1);

        assertThat(user).isSameAs(testUser);
    }

    @Test
    @DisplayName("Should throw when user by ID is missing")
    void shouldThrowWhenGetUserByIdMissing() {
        when(userRepository.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(404))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("User with id 404 not found");
    }

    @Test
    @DisplayName("Should get user by email")
    void shouldGetUserByEmail() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));

        User user = userService.getUserByEmail("test@test.com");

        assertThat(user).isSameAs(testUser);
    }

    @Test
    @DisplayName("Should throw when user by email is missing")
    void shouldThrowWhenGetUserByEmailMissing() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByEmail("missing@test.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User not found: missing@test.com");
    }

    @Test
    @DisplayName("Should check current user role")
    void shouldCheckCurrentUserRole() {
        testUser.setRoles(Set.of(new Role("USER"), new Role("TENANT")));
        authenticateAs("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        assertThat(userService.currentUserHasRole("TENANT")).isTrue();
        assertThat(userService.currentUserHasRole("ADMIN")).isFalse();
    }

    @Test
    @DisplayName("Should return current user optional when authenticated")
    void shouldReturnCurrentUserOptionalWhenAuthenticated() {
        authenticateAs("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));

        Optional<User> currentUser = userService.getCurrentUserOptional();

        assertThat(currentUser).containsSame(testUser);
    }

    @Test
    @DisplayName("Should return empty current user optional when unauthenticated")
    void shouldReturnEmptyCurrentUserOptionalWhenUnauthenticated() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        Optional<User> currentUser = userService.getCurrentUserOptional();

        assertThat(currentUser).isEmpty();
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Should not throw for non-admin user when asserting not admin")
    void shouldNotThrowForNonAdminUser() {
        userService.assertNotAdmin(testUser);
    }

    private void authenticateAs(String email) {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(
                        email, "pass", Collections.emptyList()
                )
        );
    }
}
