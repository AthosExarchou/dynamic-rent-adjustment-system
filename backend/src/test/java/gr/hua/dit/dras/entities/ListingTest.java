package gr.hua.dit.dras.entities;

/* imports */
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.model.enums.RentalStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ListingTest {

    private Listing listing;
    private Tenant tenant1;
    private Tenant tenant2;

    @BeforeEach
    void setUp() {
        listing = new Listing();
        ReflectionTestUtils.setField(listing, "id", 1);
        listing.setStatus(ListingStatus.PENDING);

        tenant1 = new Tenant();
        ReflectionTestUtils.setField(tenant1, "id", 1);

        tenant2 = new Tenant();
        ReflectionTestUtils.setField(tenant2, "id", 2);
    }

    @Test
    void testAddApplicant() {
        listing.addApplicant(tenant1);

        assertThat(listing.getApplicants()).containsExactly(tenant1);
        assertThat(tenant1.getAppliedListings()).containsExactly(listing);
    }

    @Test
    void testRemoveApplicant() {
        listing.addApplicant(tenant1);
        listing.removeApplicant(tenant1);

        assertThat(listing.getApplicants()).isEmpty();
        assertThat(tenant1.getAppliedListings()).isEmpty();
    }

    @Test
    void testApprove() {
        listing.approve();
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.APPROVED);
        assertThat(listing.isApproved()).isTrue();
    }

    @Test
    void testApproveThrowsWhenNotPending() {
        listing.setStatus(ListingStatus.APPROVED);
        assertThrows(IllegalStateException.class, () -> listing.approve());
    }

    @Test
    void testReject() {
        listing.reject();
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.REJECTED);
        assertThat(listing.isRejected()).isTrue();
    }

    @Test
    void testRejectThrowsWhenNotPending() {
        listing.setStatus(ListingStatus.REJECTED);
        assertThrows(IllegalStateException.class, () -> listing.reject());
    }

    @Test
    void testDisable() {
        listing.disable();
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.DISABLED);
        assertThat(listing.isDisabled()).isTrue();
    }

    @Test
    void testDisableThrowsWhenRented() {
        listing.setStatus(ListingStatus.RENTED);
        assertThrows(IllegalStateException.class, () -> listing.disable());
    }

    @Test
    void testMakeAvailable() {
        listing.setStatus(ListingStatus.PENDING);
        listing.makeAvailable();
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.APPROVED);
    }

    @Test
    void testMakeAvailableThrowsWhenRented() {
        listing.setStatus(ListingStatus.RENTED);
        assertThrows(IllegalStateException.class, () -> listing.makeAvailable());
    }

    @Test
    void testRentTo() {
        listing.addApplicant(tenant1);
        listing.addApplicant(tenant2);

        List<Tenant> rejectedTenants = listing.rentTo(tenant1);

        assertThat(listing.getStatus()).isEqualTo(ListingStatus.RENTED);
        assertThat(listing.getTenant()).isEqualTo(tenant1);
        assertThat(listing.getApplicants()).isEmpty();

        assertThat(rejectedTenants).containsExactly(tenant2);
        
        assertThat(tenant1.getListing()).isEqualTo(listing);
        assertThat(tenant1.getAppliedListings()).doesNotContain(listing);
        assertThat(tenant2.getAppliedListings()).doesNotContain(listing);
    }

    @Test
    void testRentToThrowsWhenTenantDidNotApply() {
        assertThrows(IllegalStateException.class, () -> listing.rentTo(tenant1));
    }

    @Test
    void testRentToThrowsWhenAlreadyRented() {
        listing.addApplicant(tenant1);
        listing.setTenant(new Tenant());

        assertThrows(IllegalStateException.class, () -> listing.rentTo(tenant1));
    }

    @Test
    void testEqualsAndHashCode() {
        Listing listing1 = new Listing();
        ReflectionTestUtils.setField(listing1, "id", 1);

        Listing listing2 = new Listing();
        ReflectionTestUtils.setField(listing2, "id", 1);

        Listing listing3 = new Listing();
        ReflectionTestUtils.setField(listing3, "id", 2);

        assertThat(listing1).isEqualTo(listing2);
        assertThat(listing1).isNotEqualTo(listing3);
        assertThat(listing1).isNotEqualTo(null);
        assertThat(listing1).isNotEqualTo(new Object());
        assertThat(listing1.hashCode()).isEqualTo(listing2.hashCode());
    }

    @Test
    @DisplayName("Should not duplicate applicant when adding same tenant twice")
    void testAddApplicantIdempotency() {
        listing.addApplicant(tenant1);
        listing.addApplicant(tenant1);

        assertThat(listing.getApplicants()).hasSize(1);
        assertThat(tenant1.getAppliedListings()).hasSize(1);
    }

    @Test
    @DisplayName("Should set tenant's rental status to RENTING when rented")
    void testRentToSetsRentalStatus() {
        listing.addApplicant(tenant1);
        
        listing.rentTo(tenant1);

        assertThat(tenant1.getRentalStatus()).isEqualTo(RentalStatus.RENTING);
    }

    @Test
    @DisplayName("Should not be equal to another listing when IDs are null")
    void testEqualsWithNullId() {
        Listing listingA = new Listing();
        Listing listingB = new Listing();

        assertThat(listingA).isNotEqualTo(listingB);
    }
}
