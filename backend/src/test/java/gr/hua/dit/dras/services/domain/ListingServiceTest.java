package gr.hua.dit.dras.services.domain;

/* imports */
import gr.hua.dit.dras.dto.ListingFilterDTO;
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.entities.Tenant;
import gr.hua.dit.dras.repositories.ListingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.springframework.security.access.AccessDeniedException;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.entities.Role;
import gr.hua.dit.dras.entities.User;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.HashSet;

@ExtendWith(MockitoExtension.class)
public class ListingServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private OwnerService ownerService;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private ListingService listingService;

    @Test
    void getListing_shouldReturnListing() {
        Listing listing = new Listing();
        ReflectionTestUtils.setField(listing, "id", 1);
        when(listingRepository.findById(1)).thenReturn(Optional.of(listing));

        Listing result = listingService.getListing(1);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
    }

    @Test
    void getListing_shouldThrowWhenNotFound() {
        when(listingRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listingService.getListing(1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void saveListing_externalWithoutDateScraped_shouldSetDateScraped() {
        Listing listing = new Listing();
        listing.setExternal(true);
        listing.setDateScraped(null);

        listingService.saveListing(listing);

        assertThat(listing.getDateScraped()).isNotNull();
        verify(listingRepository).save(listing);
    }

    @Test
    void saveListing_local_shouldClearDateScraped() {
        Listing listing = new Listing();
        listing.setExternal(false);
        listing.setDateScraped(Instant.now());

        listingService.saveListing(listing);

        assertThat(listing.getDateScraped()).isNull();
        verify(listingRepository).save(listing);
    }

    @Test
    void deleteListing_shouldThrowIfExternal() {
        Listing listing = new Listing();
        ReflectionTestUtils.setField(listing, "id", 1);
        listing.setExternal(true);
        when(listingRepository.findById(1)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> listingService.deleteListing(1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("External listings cannot be deleted manually");
    }

    @Test
    void deleteListing_shouldDeleteAndUnassign() {
        Listing listing = new Listing();
        ReflectionTestUtils.setField(listing, "id", 1);
        listing.setExternal(false);
        
        Tenant tenant = new Tenant();
        ReflectionTestUtils.setField(tenant, "id", 10);
        listing.setTenant(tenant);
        
        Owner owner = new Owner();
        ReflectionTestUtils.setField(owner, "id", 20);
        listing.setOwner(owner);

        when(listingRepository.findById(1)).thenReturn(Optional.of(listing));

        listingService.deleteListing(1);

        verify(tenantService).unassignTenantFromListing(1, 10);
        verify(ownerService).unassignOwnerFromListing(1);
        verify(listingRepository).delete(listing);
    }

    @Test
    void filterListings_shouldCallRepositoryWithSpecification() {
        ListingFilterDTO filter = new ListingFilterDTO();
        filter.setTitle("Test");
        filter.setMinPrice(100);
        filter.setMaxPrice(500);

        List<Listing> expectedListings = List.of(new Listing(), new Listing());
        when(listingRepository.findAll(anyListingSpecification())).thenReturn(expectedListings);

        List<Listing> result = listingService.filterListings(filter);

        assertThat(result).isEqualTo(expectedListings);
        verify(listingRepository).findAll(anyListingSpecification());
    }

    @Test
    void filterListings_shouldThrowOnInvalidPriceRange() {
        ListingFilterDTO filter = new ListingFilterDTO();
        filter.setMinPrice(500);
        filter.setMaxPrice(100);

        assertThatThrownBy(() -> listingService.filterListings(filter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid price range");
        verify(listingRepository, never()).findAll(anyListingSpecification());
    }

    @Test
    void filterListings_shouldThrowOnInvalidUpdatedDateRange() {
        ListingFilterDTO filter = new ListingFilterDTO();
        filter.setUpdatedAfter(LocalDate.of(2026, 6, 20));
        filter.setUpdatedBefore(LocalDate.of(2026, 6, 19));

        assertThatThrownBy(() -> listingService.filterListings(filter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid date range");
        verify(listingRepository, never()).findAll(anyListingSpecification());
    }

    @Test
    @DisplayName("Should approve listing")
    void shouldApproveListing() {
        Listing listing = new Listing();
        ReflectionTestUtils.setField(listing, "id", 1);
        listing.setStatus(ListingStatus.PENDING);
        
        when(listingRepository.findById(1)).thenReturn(Optional.of(listing));

        listingService.approveListing(1);

        assertThat(listing.getStatus()).isEqualTo(ListingStatus.APPROVED);
        verify(listingRepository).save(listing);
    }

    @Test
    @DisplayName("Should reject listing")
    void shouldRejectListing() {
        Listing listing = new Listing();
        ReflectionTestUtils.setField(listing, "id", 1);
        listing.setStatus(ListingStatus.PENDING);
        
        when(listingRepository.findById(1)).thenReturn(Optional.of(listing));

        listingService.rejectListing(1);

        assertThat(listing.getStatus()).isEqualTo(ListingStatus.REJECTED);
        verify(listingRepository).save(listing);
    }

    @Test
    @DisplayName("Should allow admin to modify any listing")
    void shouldAllowAdminToModifyAnyListing() {
        User adminUser = new User();
        adminUser.setRoles(Set.of(new Role("ADMIN")));
        Listing listing = new Listing();

        listingService.validateListingModificationRights(listing, adminUser);
    }

    @Test
    @DisplayName("Should allow listing owner to modify available listing")
    void shouldAllowOwnerToModifyOwnAvailableListing() {
        User ownerUser = new User();
        ReflectionTestUtils.setField(ownerUser, "id", 1);
        ownerUser.setRoles(Set.of(new Role("OWNER")));

        Owner owner = new Owner();
        owner.setUser(ownerUser);

        Listing listing = new Listing();
        listing.setStatus(ListingStatus.APPROVED);
        listing.setOwner(owner);

        listingService.validateListingModificationRights(listing, ownerUser);
    }

    @Test
    @DisplayName("Should throw when non-owner modifies listing")
    void shouldThrowWhenNonOwnerModifiesListing() {
        User regularUser = new User();
        regularUser.setRoles(Set.of(new Role("USER")));
        Listing listing = new Listing();

        assertThatThrownBy(() -> listingService.validateListingModificationRights(listing, regularUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Not an owner");
    }

    @Test
    @DisplayName("Should throw when modifying rented listing")
    void shouldThrowWhenModifyingRentedListing() {
        User ownerUser = new User();
        ReflectionTestUtils.setField(ownerUser, "id", 1);
        ownerUser.setRoles(Set.of(new Role("OWNER")));
        
        Owner owner = new Owner();
        owner.setUser(ownerUser);
        
        Listing listing = new Listing();
        listing.setStatus(ListingStatus.RENTED);
        listing.setOwner(owner);

        assertThatThrownBy(() -> listingService.validateListingModificationRights(listing, ownerUser))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot modify a rented listing");
    }

    @Test
    @DisplayName("Should reject applicant from listing")
    void shouldRejectApplicant() {
        Listing listing = new Listing();
        Tenant tenant = new Tenant();
        ReflectionTestUtils.setField(tenant, "id", 1);
        
        listing.setApplicants(new HashSet<>());
        listing.addApplicant(tenant);

        listingService.rejectApplicant(listing, tenant);

        assertThat(listing.getApplicants()).doesNotContain(tenant);
        assertThat(tenant.getAppliedListings()).doesNotContain(listing);
        verify(listingRepository).save(listing);
    }

    @Test
    @DisplayName("Should throw when rejecting non-applicant")
    void shouldThrowWhenRejectingNonApplicant() {
        Listing listing = new Listing();
        listing.setApplicants(new HashSet<>());
        Tenant tenant = new Tenant();

        assertThatThrownBy(() -> listingService.rejectApplicant(listing, tenant))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Tenant did not apply for this listing");
    }

    @Test
    @DisplayName("Should delete listing with no tenant and no owner")
    void shouldDeleteListingWithNoTenantAndNoOwner() {
        Listing listing = new Listing();
        ReflectionTestUtils.setField(listing, "id", 1);
        listing.setExternal(false);
        listing.setTenant(null);
        listing.setOwner(null);

        when(listingRepository.findById(1)).thenReturn(Optional.of(listing));

        listingService.deleteListing(1);

        verify(listingRepository).delete(listing);
        verifyNoInteractions(tenantService, ownerService);
    }

    @SuppressWarnings("unchecked")
    private Specification<Listing> anyListingSpecification() {
        return any(Specification.class);
    }
}
