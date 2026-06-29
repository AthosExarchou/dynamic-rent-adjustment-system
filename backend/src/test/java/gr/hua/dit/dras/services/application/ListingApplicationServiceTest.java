package gr.hua.dit.dras.services.application;

/* imports */
import gr.hua.dit.dras.entities.*;
import gr.hua.dit.dras.model.enums.ListingStatus;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.HashSet;
import java.util.Set;

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
    @DisplayName("Should reject listing and send email")
    void shouldRejectListing() {
        when(listingService.getListing(100)).thenReturn(testListing);

        listingApplicationService.rejectListing(100);

        verify(listingService).rejectListing(100);
        verify(emailService).sendEmailNotification(
                "test@test.com", "Owner Name", testListing, "adminRejected");
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
}
