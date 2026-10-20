package gr.hua.dit.dras.services.application;

/* imports */
import gr.hua.dit.dras.dto.AccountDeletionRequest;
import gr.hua.dit.dras.entities.Notification;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.entities.Role;
import gr.hua.dit.dras.entities.Tenant;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.services.domain.ListingService;
import gr.hua.dit.dras.services.domain.NotificationService;
import gr.hua.dit.dras.services.domain.TenantService;
import gr.hua.dit.dras.services.domain.UserService;
import gr.hua.dit.dras.services.infrastructure.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserApplicationServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @Mock
    private ListingService listingService;
    @Mock
    private TenantService tenantService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserApplicationService userApplicationService;

    private User adminUser;
    private User targetUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        ReflectionTestUtils.setField(adminUser, "id", 1);
        adminUser.setEmail("admin@gmail.com");
        Set<Role> adminRoles = new HashSet<>();
        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        adminRoles.add(adminRole);
        adminUser.setRoles(adminRoles);

        targetUser = new User();
        ReflectionTestUtils.setField(targetUser, "id", 4);
        targetUser.setEmail("testuser44new5@gmail.com");
        targetUser.setPassword("encodedPassword123");
        Set<Role> userRoles = new HashSet<>();
        Role userRole = new Role();
        userRole.setName("USER");
        userRoles.add(userRole);
        targetUser.setRoles(userRoles);

        List<Notification> notifications = new ArrayList<>();
        Notification n = new Notification();
        n.setId(101);
        n.setUser(targetUser);
        notifications.add(n);
        targetUser.setNotifications(notifications);
    }

    @Test
    @DisplayName("Should successfully delete user as admin and clean up notifications footprint")
    void deleteUserAsAdmin_cleansUpNotificationsAndDeletes() {
        when(userService.getCurrentUserOptional()).thenReturn(Optional.of(adminUser));
        when(userService.getUser(4)).thenReturn(targetUser);

        userApplicationService.deleteUserAsAdmin(4);

        verify(notificationService).deleteNotificationsForUser(4);
        verify(userService).deleteUser(4);
        assertThat(targetUser.getNotifications()).isEmpty();
    }

    @Test
    @DisplayName("Should clean up owner listings and notifications when target is an owner")
    void deleteUserAsAdmin_ownerFootprintCleaned() {
        Owner owner = new Owner();
        ReflectionTestUtils.setField(owner, "id", 20);
        owner.setUser(targetUser);
        targetUser.setOwner(owner);

        when(userService.getCurrentUserOptional()).thenReturn(Optional.of(adminUser));
        when(userService.getUser(4)).thenReturn(targetUser);

        userApplicationService.deleteUserAsAdmin(4);

        verify(listingService).deleteAllListingsForOwner(owner);
        verify(notificationService).deleteNotificationsForUser(4);
        verify(userService).deleteUser(4);
    }

    @Test
    @DisplayName("Should clean up tenant profile and notifications when target is a tenant")
    void deleteUserAsAdmin_tenantFootprintCleaned() {
        Tenant tenant = new Tenant();
        ReflectionTestUtils.setField(tenant, "id", 30);
        tenant.setUser(targetUser);
        targetUser.setTenant(tenant);

        when(userService.getCurrentUserOptional()).thenReturn(Optional.of(adminUser));
        when(userService.getUser(4)).thenReturn(targetUser);

        userApplicationService.deleteUserAsAdmin(4);

        verify(tenantService).prepareTenantForDeletion(tenant);
        verify(notificationService).deleteNotificationsForUser(4);
        verify(userService).deleteUser(4);
    }

    @Test
    @DisplayName("Should clean up notifications on self account deletion")
    void deleteCurrentUserAccount_cleansUpNotificationsAndDeletes() {
        when(userService.getCurrentUserOptional()).thenReturn(Optional.of(targetUser));
        when(passwordEncoder.matches("rawPassword123", "encodedPassword123")).thenReturn(true);

        AccountDeletionRequest request = new AccountDeletionRequest();
        request.setPassword("rawPassword123");
        request.setConfirmationPhrase("DELETE MY ACCOUNT");

        userApplicationService.deleteCurrentUserAccount(request);

        verify(notificationService).deleteNotificationsForUser(4);
        verify(userService).deleteUser(4);
        assertThat(targetUser.getNotifications()).isEmpty();
    }
}
