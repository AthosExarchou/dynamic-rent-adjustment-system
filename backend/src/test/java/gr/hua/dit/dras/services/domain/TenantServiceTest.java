package gr.hua.dit.dras.services.domain;

/* imports */
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.entities.Role;
import gr.hua.dit.dras.entities.Tenant;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.model.enums.RentalStatus;
import gr.hua.dit.dras.repositories.ListingRepository;
import gr.hua.dit.dras.repositories.RoleRepository;
import gr.hua.dit.dras.repositories.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.DisplayName;
import java.util.Set;
import java.util.HashSet;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserService userService;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private TenantService tenantService;

    @Test
    void getTenant_withId_shouldReturnTenant() {
        Tenant tenant = new Tenant();
        tenant.setId(1);
        when(tenantRepository.findById(1)).thenReturn(Optional.of(tenant));

        Tenant result = tenantService.getTenant(1);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        verify(tenantRepository).findById(1);
    }

    @Test
    void getTenant_withoutId_shouldReturnTenantForCurrentUser() {
        Tenant tenant = new Tenant();
        tenant.setId(2);
        when(userService.getCurrentUserId()).thenReturn(10);
        when(tenantRepository.findByUserId(10)).thenReturn(Optional.of(tenant));

        Tenant result = tenantService.getTenant(null);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2);
        verify(userService).getCurrentUserId();
        verify(tenantRepository).findByUserId(10);
    }

    @Test
    void createTenantForUser_shouldCreateAndReturnTenant() {
        User user = new User();
        user.setId(1);
        user.setRoles(new HashSet<>());
        
        Role role = new Role();
        role.setName("TENANT");
        
        when(userService.getUser(1)).thenReturn(user);
        when(tenantRepository.findByUserId(1)).thenReturn(Optional.empty());
        when(roleRepository.findByName("TENANT")).thenReturn(Optional.of(role));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> {
            Tenant t = invocation.getArgument(0);
            t.setId(100);
            return t;
        });

        Tenant result = tenantService.createTenantForUser(
                1, "John", "Doe", "1234567890");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100);
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getPhoneNumber()).isEqualTo("1234567890");
        assertThat(result.getRentalStatus()).isEqualTo(RentalStatus.APPLIED);
        assertThat(result.getUser()).isEqualTo(user);
        
        verify(tenantRepository).save(any(Tenant.class));
        verify(userService).updateUser(user);
        assertThat(user.getRoles()).contains(role);
    }

    @Test
    void submitApplication_shouldApplyToListing() {
        Tenant tenant = new Tenant();
        tenant.setId(1);
        tenant.setListing(null);

        Listing listing = new Listing();
        listing.setId(10);
        listing.setApplicants(new HashSet<>());

        when(userService.getCurrentUserId()).thenReturn(5);
        when(tenantRepository.findByUserId(5)).thenReturn(Optional.of(tenant));
        when(listingRepository.findById(10)).thenReturn(Optional.of(listing));

        boolean result = tenantService.submitApplication(10);

        assertThat(result).isFalse();
        assertThat(tenant.getRentalStatus()).isEqualTo(RentalStatus.APPLIED);
        assertThat(tenant.getAppliedListings()).containsExactly(listing);
        assertThat(listing.getApplicants()).containsExactly(tenant);
        verify(tenantRepository).save(tenant);
        verify(listingRepository).save(listing);
    }

    @Test
    void unassignTenantFromListing_shouldUnassign() {
        Tenant tenant = new Tenant();
        tenant.setId(1);
        
        Listing listing = new Listing();
        listing.setId(10);
        listing.setTenant(tenant);
        
        when(listingRepository.findById(10)).thenReturn(Optional.of(listing));
        when(tenantRepository.findById(1)).thenReturn(Optional.of(tenant));

        tenantService.unassignTenantFromListing(10, 1);

        assertThat(listing.getTenant()).isNull();
        assertThat(tenant.getRentalStatus()).isEqualTo(RentalStatus.CANCELED);
        verify(tenantRepository).save(tenant);
        verify(listingRepository).save(listing);
    }

    @Test
    void validateRentalApplicationRights_shouldThrowWhenUserIsNull() {
        Listing listing = new Listing();
        assertThatThrownBy(() -> tenantService.validateRentalApplicationRights(null, listing))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Unauthenticated");
    }

    @Test
    void validateRentalApplicationRights_shouldThrowWhenUserIsAdmin() {
        User user = new User();
        Listing listing = new Listing();
        when(userService.currentUserHasRole("ADMIN")).thenReturn(true);

        assertThatThrownBy(() -> tenantService.validateRentalApplicationRights(user, listing))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Admins cannot rent listings");
    }

    @Test
    void validateRentalApplicationRights_shouldThrowWhenListingNotApproved() {
        User user = new User();
        Listing listing = new Listing();
        listing.setStatus(gr.hua.dit.dras.model.enums.ListingStatus.PENDING);
        when(userService.currentUserHasRole("ADMIN")).thenReturn(false);

        assertThatThrownBy(() -> tenantService.validateRentalApplicationRights(user, listing))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Listing not available for rental");
    }

    @Test
    void validateRentalApplicationRights_shouldThrowWhenListingIsRented() {
        User user = new User();
        Listing listing = new Listing();
        listing.setStatus(gr.hua.dit.dras.model.enums.ListingStatus.RENTED);
        when(userService.currentUserHasRole("ADMIN")).thenReturn(false);

        assertThatThrownBy(() -> tenantService.validateRentalApplicationRights(user, listing))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Listing already rented");
    }

    @Test
    void validateRentalApplicationRights_shouldThrowWhenUserIsOwner() {
        User user = new User();
        user.setId(1);
        Listing listing = new Listing();
        listing.setStatus(gr.hua.dit.dras.model.enums.ListingStatus.APPROVED);
        Owner owner = new Owner();
        owner.setUser(user);
        listing.setOwner(owner);
        when(userService.currentUserHasRole("ADMIN")).thenReturn(false);

        assertThatThrownBy(() -> tenantService.validateRentalApplicationRights(user, listing))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Owners cannot rent their own listings");
    }

    @Test
    @DisplayName("Should throw IllegalStateException when creating duplicate tenant profile")
    void shouldThrowWhenCreatingDuplicateTenantProfile() {
        User user = new User();
        user.setId(1);
        when(userService.getUser(1)).thenReturn(user);
        when(tenantRepository.findByUserId(1)).thenReturn(Optional.of(new Tenant()));

        assertThatThrownBy(() -> tenantService.createTenantForUser(
                1, "John", "Doe", "1234567890"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User already has a Tenant profile");
    }

    @Test
    @DisplayName("Should return true when tenant already applied for listing")
    void shouldReturnTrueWhenTenantAlreadyApplied() {
        Tenant tenant = new Tenant();
        tenant.setId(1);

        Listing listing = new Listing();
        listing.setId(10);
        
        Set<Tenant> applicants = new HashSet<>();
        applicants.add(tenant);
        listing.setApplicants(applicants);

        when(userService.getCurrentUserId()).thenReturn(5);
        when(tenantRepository.findByUserId(5)).thenReturn(Optional.of(tenant));
        when(listingRepository.findById(10)).thenReturn(Optional.of(listing));

        boolean result = tenantService.submitApplication(10);

        assertThat(result).isTrue();
        verify(tenantRepository, never()).save(any());
        verify(listingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when tenant already renting on submit application")
    void shouldThrowWhenTenantAlreadyRentingOnSubmitApplication() {
        Tenant tenant = new Tenant();
        tenant.setId(1);
        tenant.setListing(new Listing()); // already renting

        Listing listing = new Listing();
        listing.setId(10);
        listing.setApplicants(new HashSet<>());

        when(userService.getCurrentUserId()).thenReturn(5);
        when(tenantRepository.findByUserId(5)).thenReturn(Optional.of(tenant));
        when(listingRepository.findById(10)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> tenantService.submitApplication(10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already renting a listing");
        verify(tenantRepository, never()).save(any());
        verify(listingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when tenant not found by ID")
    void shouldThrowWhenTenantNotFound() {
        when(tenantRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.getTenant(999))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Tenant not found with id: 999");
    }

    @Test
    @DisplayName("Should pass validation for APPROVED listing with non-owner user")
    void shouldPassValidationForApprovedListingWithNonOwnerUser() {
        User user = new User();
        user.setId(1);

        User ownerUser = new User();
        ownerUser.setId(2);
        
        Owner owner = new Owner();
        owner.setUser(ownerUser);

        Listing listing = new Listing();
        listing.setStatus(gr.hua.dit.dras.model.enums.ListingStatus.APPROVED);
        listing.setOwner(owner);

        when(userService.currentUserHasRole("ADMIN")).thenReturn(false);

        // Should not throw any exception
        tenantService.validateRentalApplicationRights(user, listing);
    }
}
