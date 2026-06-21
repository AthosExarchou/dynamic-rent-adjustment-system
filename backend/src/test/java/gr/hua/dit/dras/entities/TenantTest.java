package gr.hua.dit.dras.entities;

/* imports */
import gr.hua.dit.dras.model.enums.RentalStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantTest {

    private Tenant tenant;
    private Listing listing1;
    private Listing listing2;

    @BeforeEach
    void setUp() {
        tenant = new Tenant("John", "Doe", "+1234567890");
        tenant.setId(1);

        listing1 = new Listing();
        listing1.setId(1);

        listing2 = new Listing();
        listing2.setId(2);
    }

    @Test
    void testApplyToListing() {
        tenant.applyToListing(listing1);

        assertThat(tenant.getAppliedListings()).containsExactly(listing1);
        assertThat(listing1.getApplicants()).containsExactly(tenant);
    }

    @Test
    void testRent() {
        tenant.applyToListing(listing1);
        tenant.rent(listing1);

        assertThat(tenant.getListing()).isEqualTo(listing1);
        assertThat(tenant.getRentalStatus()).isEqualTo(RentalStatus.RENTING);
        assertThat(tenant.getAppliedListings()).doesNotContain(listing1);
    }

    @Test
    void testRentThrowsWhenAlreadyRenting() {
        tenant.setListing(listing2);

        assertThrows(IllegalStateException.class, () -> tenant.rent(listing1));
    }

    @Test
    void testProcessRejectionChangesStatusWhenNoOtherApplications() {
        tenant.applyToListing(listing1);
        
        tenant.processRejection(listing1);

        assertThat(tenant.getAppliedListings()).doesNotContain(listing1);
        assertThat(tenant.getRentalStatus()).isEqualTo(RentalStatus.CANCELED);
    }

    @Test
    void testProcessRejectionDoesNotChangeStatusWhenOtherApplicationsExist() {
        tenant.applyToListing(listing1);
        tenant.applyToListing(listing2);
        
        tenant.processRejection(listing1);

        assertThat(tenant.getAppliedListings()).doesNotContain(listing1);
        assertThat(tenant.getAppliedListings()).contains(listing2);
        assertThat(tenant.getRentalStatus()).isEqualTo(RentalStatus.APPLIED);
    }
    
    @Test
    void testProcessRejectionDoesNotChangeStatusWhenAlreadyRenting() {
        tenant.applyToListing(listing1);
        tenant.setListing(listing2);
        tenant.setRentalStatus(RentalStatus.RENTING);

        tenant.processRejection(listing1);

        assertThat(tenant.getAppliedListings()).doesNotContain(listing1);
        assertThat(tenant.getRentalStatus()).isEqualTo(RentalStatus.RENTING);
    }

    @Test
    void testEqualsAndHashCode() {
        Tenant tenant1 = new Tenant();
        tenant1.setId(1);

        Tenant tenant2 = new Tenant();
        tenant2.setId(1);

        Tenant tenant3 = new Tenant();
        tenant3.setId(2);

        assertThat(tenant1).isEqualTo(tenant2);
        assertThat(tenant1).isNotEqualTo(tenant3);
        assertThat(tenant1).isNotEqualTo(null);
        assertThat(tenant1).isNotEqualTo(new Object());
        assertThat(tenant1.hashCode()).isEqualTo(tenant2.hashCode());
    }
}
