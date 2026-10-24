package gr.hua.dit.dras.services.application;

/* imports */
import gr.hua.dit.dras.dto.ListingCreateDTO;
import gr.hua.dit.dras.entities.*;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.model.enums.PropertyType;
import gr.hua.dit.dras.model.enums.RentalDuration;
import gr.hua.dit.dras.repositories.RoleRepository;
import gr.hua.dit.dras.services.domain.*;
import gr.hua.dit.dras.services.infrastructure.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.servlet.http.HttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingApplicationServiceTest {

    @Mock
    private ListingService listingService;
    @Mock
    private OwnerService ownerService;
    @Mock
    private TenantService tenantService;
    @Mock
    private UserService userService;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private ListingApplicationService listingApplicationService;

    private User testUser;
    private Listing testListing;
    private Tenant testTenant;
    private Owner testOwner;

    @BeforeEach
    void setUp() {
        testUser = new User();
        ReflectionTestUtils.setField(testUser, "id", 1);
        testUser.setEmail("test@test.com");
        testUser.setRoles(new HashSet<>());

        testOwner = new Owner();
        ReflectionTestUtils.setField(testOwner, "id", 10);
        testOwner.setUser(testUser);
        testOwner.setFirstName("Owner");
        testOwner.setLastName("Name");

        testListing = new Listing();
        ReflectionTestUtils.setField(testListing, "id", 100);
        testListing.setStatus(ListingStatus.PENDING);
        testListing.setOwner(testOwner);
        testListing.setApplicants(new HashSet<>());

        testTenant = new Tenant();
        ReflectionTestUtils.setField(testTenant, "id", 5);
        testTenant.setUser(testUser);
        testTenant.setFirstName("Tenant");
        testTenant.setLastName("Name");
    }

    @Test
    @DisplayName("Should approve listing and send email")
    void shouldApproveListing() {
        when(listingService.getListing(100)).thenReturn(testListing);

        listingApplicationService.approveListing(100);

        verify(listingService).approveListing(100);
        verify(emailService).sendEmailNotification(
                "test@test.com", "Owner Name", testListing, "adminApproved");
    }

    @Test
    @DisplayName("Should reject listing with reason and send email")
    void shouldRejectListing() {
        testListing.setStatus(ListingStatus.REJECTED);
        testListing.setRejectionReason("Incomplete address and incorrect price");
        when(listingService.rejectListing(100, "Incomplete address and incorrect price")).thenReturn(testListing);

        listingApplicationService.rejectListing(100, "Incomplete address and incorrect price");

        verify(listingService).rejectListing(100, "Incomplete address and incorrect price");
        verify(emailService).sendEmailNotification(
                "test@test.com", "Owner Name", testListing, "adminRejected");
    }

    @Test
    @DisplayName("Should reject listing with default reason when overloaded method called")
    void shouldRejectListingWithDefaultReason() {
        testListing.setStatus(ListingStatus.REJECTED);
        testListing.setRejectionReason("Listing does not meet publication criteria");
        when(listingService.rejectListing(100, "Listing does not meet publication criteria")).thenReturn(testListing);

        listingApplicationService.rejectListing(100);

        verify(listingService).rejectListing(100, "Listing does not meet publication criteria");
        verify(emailService).sendEmailNotification(
                "test@test.com", "Owner Name", testListing, "adminRejected");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when rejection reason is null or blank")
    void shouldThrowWhenRejectionReasonIsBlank() {
        assertThatThrownBy(() -> listingApplicationService.rejectListing(100, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rejection reason is required");

        assertThatThrownBy(() -> listingApplicationService.rejectListing(100, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rejection reason is required");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when rejection reason exceeds 500 characters")
    void shouldThrowWhenRejectionReasonExceeds500Characters() {
        String longReason = "a".repeat(501);

        assertThatThrownBy(() -> listingApplicationService.rejectListing(100, longReason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rejection reason cannot exceed 500 characters");
    }

    @Test
    @DisplayName("Should disable listing when requested by authorized owner")
    void shouldDisableListing() {
        when(userService.getCurrentUserOptional()).thenReturn(Optional.of(testUser));
        when(listingService.getListing(100)).thenReturn(testListing);

        listingApplicationService.disableListing(100);

        verify(listingService).validateListingModificationRights(testListing, testUser);
        verify(listingService).disableListing(100);
    }

    @Test
    @DisplayName("Should enable listing when requested by authorized owner")
    void shouldEnableListing() {
        when(userService.getCurrentUserOptional()).thenReturn(Optional.of(testUser));
        when(listingService.getListing(100)).thenReturn(testListing);

        listingApplicationService.enableListing(100);

        verify(listingService).validateListingModificationRights(testListing, testUser);
        verify(listingService).enableListing(100);
    }

    @Test
    @DisplayName("Should toggle listing status between disabled and approved")
    void shouldToggleListingStatus() {
        when(userService.getCurrentUserOptional()).thenReturn(Optional.of(testUser));
        when(listingService.getListing(100)).thenReturn(testListing);
        testListing.setStatus(ListingStatus.APPROVED);

        listingApplicationService.toggleListingStatus(100);
        verify(listingService).disableListing(100);

        testListing.setStatus(ListingStatus.DISABLED);
        listingApplicationService.toggleListingStatus(100);
        verify(listingService).enableListing(100);
    }

    @Test
    @DisplayName("Should approve tenant application, reject other applicants, grant role, and notify")
    void shouldApproveTenantApplication() {
        User ownerUser = new User();
        ReflectionTestUtils.setField(ownerUser, "id", 1);
        ownerUser.setRoles(Set.of(new Role("OWNER")));

        User tenantUser = new User();
        ReflectionTestUtils.setField(tenantUser, "id", 2);
        tenantUser.setEmail("tenant@test.com");
        tenantUser.setRoles(new HashSet<>());

        User rejectedUser = new User();
        ReflectionTestUtils.setField(rejectedUser, "id", 3);
        rejectedUser.setEmail("rejected@test.com");

        Owner owner = new Owner();
        owner.setUser(ownerUser);

        Tenant approvedTenant = new Tenant("Approved", "Tenant", "1234567890");
        ReflectionTestUtils.setField(approvedTenant, "id", 5);
        approvedTenant.setUser(tenantUser);

        Tenant rejectedTenant = new Tenant("Rejected", "Tenant", "1234567891");
        ReflectionTestUtils.setField(rejectedTenant, "id", 6);
        rejectedTenant.setUser(rejectedUser);

        Listing listing = new Listing();
        ReflectionTestUtils.setField(listing, "id", 100);
        listing.setStatus(ListingStatus.APPROVED);
        listing.setOwner(owner);
        listing.addApplicant(approvedTenant);
        listing.addApplicant(rejectedTenant);

        Role tenantRole = new Role("TENANT");

        when(userService.getCurrentUserOptional()).thenReturn(Optional.of(ownerUser));
        when(listingService.getListing(100)).thenReturn(listing);
        when(tenantService.getTenant(5)).thenReturn(approvedTenant);
        when(roleRepository.findByName("TENANT")).thenReturn(Optional.of(tenantRole));

        listingApplicationService.approveTenantApplication(100, 5);

        assertThat(listing.getTenant()).isEqualTo(approvedTenant);
        assertThat(listing.getApplicants()).isEmpty();
        assertThat(approvedTenant.getRentalStatus()).isEqualTo(gr.hua.dit.dras.model.enums.RentalStatus.RENTING);
        assertThat(rejectedTenant.getAppliedListings()).doesNotContain(listing);
        assertThat(tenantUser.getRoles()).contains(tenantRole);
        verify(listingService).validateListingModificationRights(listing, ownerUser);
        verify(userService).updateUser(tenantUser);
        verify(emailService).sendEmailNotification(
                "tenant@test.com", "Approved Tenant", listing, "tenantApproval");
        verify(emailService).sendEmailNotification(
                "rejected@test.com", "Rejected Tenant", listing, "listingRentedToSomeoneElse");
    }

    @Test
    @DisplayName("Should reject tenant application via listing service")
    void shouldRejectTenantApplication() {
        when(userService.getCurrentUserOptional()).thenReturn(Optional.of(testUser));
        when(listingService.getListing(100)).thenReturn(testListing);
        when(tenantService.getTenant(5)).thenReturn(testTenant);
        
        listingApplicationService.rejectTenantApplication(100, 5);

        verify(listingService).validateListingModificationRights(testListing, testUser);
        verify(listingService).rejectApplicant(testListing, testTenant);
        verify(emailService).sendEmailNotification(
                "test@test.com", "Tenant Name", testListing, "ownerRejectedApplication");
    }

    @Test
    @DisplayName("Should throw IllegalStateException when not authenticated")
    void shouldThrowWhenNotAuthenticated() {
        when(userService.getCurrentUserOptional()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listingApplicationService.approveTenantApplication(100, 5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Authentication required");
        verifyNoInteractions(listingService, tenantService, emailService);
    }

    @Test
    @DisplayName("Should throw when deleting listing with active rental")
    void shouldThrowWhenDeletingRentedListing() {
        testListing.setTenant(testTenant); // Simulate active rental
        
        when(userService.getCurrentUserOptional()).thenReturn(Optional.of(testUser));
        when(listingService.getListing(100)).thenReturn(testListing);

        assertThatThrownBy(() -> listingApplicationService.deleteListing(100))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Listing has an active rental");
        verify(listingService, never()).deleteListing(anyInt());
    }

    @Test
    @DisplayName("Should create listing for owner without invalidating session")
    void shouldCreateListingWithoutInvalidatingSession() {
        HttpSession mockSession = mock(HttpSession.class);

        when(userService.getCurrentUserOptional()).thenReturn(Optional.of(testUser));
        when(userService.isUserOwner()).thenReturn(true);
        when(ownerService.getOwner(null)).thenReturn(testOwner);
        when(listingService.isFirstListing(testOwner)).thenReturn(true);
        when(roleRepository.findByName("OWNER")).thenReturn(Optional.of(new Role("OWNER")));

        Listing newListing = new Listing();
        newListing.setTitle("New Property");

        listingApplicationService.createListing(newListing, null, null, null, null, mockSession);

        verify(mockSession, never()).invalidate();
        verify(listingService).saveListing(newListing);
        verify(ownerService).assignOwnerToListing(newListing.getId(), testOwner);
    }

    @Test
    @DisplayName("Should view applications for listing by validating ownership")
    void shouldViewApplicationsForListing() {
        when(userService.getCurrentUserOptional()).thenReturn(Optional.of(testUser));
        when(listingService.getListing(100)).thenReturn(testListing);

        Listing result = listingApplicationService.viewApplications(100);

        assertThat(result).isEqualTo(testListing);
        verify(listingService).validateListingOwnership(testListing, testUser);
    }

    @Test
    @DisplayName("Should edit listing successfully, reset REJECTED to PENDING, and notify admins")
    void shouldEditListing() {
        testListing.setStatus(ListingStatus.REJECTED);

        ListingCreateDTO dto = new ListingCreateDTO();
        dto.setTitle("Updated Title");
        dto.setSubtitle("Updated Subtitle");
        dto.setDescription("Updated Description");
        dto.setPrice(750);
        dto.setPricePerM2(12);
        dto.setAddress("Updated St 456");
        dto.setPropertyType(PropertyType.HOUSE);
        dto.setRentalDuration(RentalDuration.SHORT_TERM);
        dto.setYearBuilt(2015);
        dto.setSizeM2(85);
        dto.setFloor(3);
        dto.setBedrooms(2);
        dto.setBathrooms(1);
        dto.setImages(List.of("https://example.com/updated.jpg"));

        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@test.com");

        when(userService.getCurrentUserOptional()).thenReturn(Optional.of(testUser));
        when(listingService.getListing(100)).thenReturn(testListing);
        when(listingService.updateListing(testListing)).thenReturn(testListing);
        when(userService.getAdminUsers()).thenReturn(List.of(adminUser));

        Listing updated = listingApplicationService.editListing(100, dto);

        assertThat(updated).isEqualTo(testListing);
        assertThat(testListing.getTitle()).isEqualTo("Updated Title");
        assertThat(testListing.getSubtitle()).isEqualTo("Updated Subtitle");
        assertThat(testListing.getPrice()).isEqualTo(750);
        assertThat(testListing.getStatus()).isEqualTo(ListingStatus.PENDING);
        assertThat(testListing.getImages()).containsExactly("https://example.com/updated.jpg");

        verify(listingService).validateListingModificationRights(testListing, testUser);
        verify(listingService).updateListing(testListing);
        verify(emailService).sendEmailNotification("admin@test.com", "admin", testListing, "listingResubmittedAdmin");
    }

    @Test
    @DisplayName("Should reset APPROVED to PENDING when owner edits, without notifying admin")
    void shouldResetApprovedToPendingWhenOwnerEdits() {
        testListing.setStatus(ListingStatus.APPROVED);

        ListingCreateDTO dto = new ListingCreateDTO();
        dto.setTitle("Minor Update");
        dto.setDescription("Minor update desc");
        dto.setPrice(650);
        dto.setAddress("Athens 123");
        dto.setSizeM2(50);
        dto.setPropertyType(PropertyType.APARTMENT);
        dto.setRentalDuration(RentalDuration.LONG_TERM);

        when(userService.getCurrentUserOptional()).thenReturn(Optional.of(testUser));
        when(listingService.getListing(100)).thenReturn(testListing);
        when(listingService.updateListing(testListing)).thenReturn(testListing);

        Listing updated = listingApplicationService.editListing(100, dto);

        assertThat(updated.getStatus()).isEqualTo(ListingStatus.PENDING);
        verify(emailService, never()).sendEmailNotification(any(), any(), any(), eq("listingResubmittedAdmin"));
    }

    @Test
    @DisplayName("Should preserve APPROVED status when admin edits an approved listing")
    void shouldPreserveApprovedStatusWhenAdminEdits() {
        testListing.setStatus(ListingStatus.APPROVED);

        User adminUser = new User();
        ReflectionTestUtils.setField(adminUser, "id", 99);
        adminUser.setRoles(Set.of(new Role("ADMIN")));

        ListingCreateDTO dto = new ListingCreateDTO();
        dto.setTitle("Admin Typo Fix");
        dto.setDescription("Admin updated desc");
        dto.setPrice(650);
        dto.setAddress("Athens 123");
        dto.setSizeM2(50);
        dto.setPropertyType(PropertyType.APARTMENT);
        dto.setRentalDuration(RentalDuration.LONG_TERM);

        when(userService.getCurrentUserOptional()).thenReturn(Optional.of(adminUser));
        when(listingService.getListing(100)).thenReturn(testListing);
        when(listingService.updateListing(testListing)).thenReturn(testListing);

        Listing updated = listingApplicationService.editListing(100, dto);

        assertThat(updated.getStatus()).isEqualTo(ListingStatus.APPROVED);
        verify(emailService, never()).sendEmailNotification(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when admin attempts to create listing")
    void shouldThrowWhenAdminAttemptsToCreateListing() {
        User adminUser = new User();
        ReflectionTestUtils.setField(adminUser, "id", 99);
        adminUser.setRoles(Set.of(new Role("ADMIN")));

        when(userService.getCurrentUserOptional()).thenReturn(Optional.of(adminUser));

        Listing newListing = new Listing();

        assertThatThrownBy(() -> listingApplicationService
                .createListing(newListing, null, null, null, null, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Administrators are not permitted to create listings");

        verifyNoInteractions(listingService, ownerService);
    }
}
