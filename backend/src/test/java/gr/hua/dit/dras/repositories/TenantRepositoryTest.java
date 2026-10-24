package gr.hua.dit.dras.repositories;

/* imports */
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.Tenant;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.model.enums.PropertyType;
import gr.hua.dit.dras.model.enums.RentalDuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TenantRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TenantRepository tenantRepository;

    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("ValidPassword123!");
        return entityManager.persistAndFlush(user);
    }

    private Listing createListing(String title, String address) {
        Listing listing = new Listing();
        listing.setTitle(title);
        listing.setDescription("Spacious test apartment");
        listing.setPrice(650);
        listing.setPricePerM2(10);
        listing.setAddress(address);
        listing.setSizeM2(65);
        listing.setPropertyType(PropertyType.APARTMENT);
        listing.setRentalDuration(RentalDuration.LONG_TERM);
        listing.setStatus(ListingStatus.APPROVED);
        return entityManager.persistAndFlush(listing);
    }

    private Tenant createTenant(User user, String firstName, String lastName, String phone) {
        Tenant tenant = new Tenant(firstName, lastName, phone);
        tenant.setUser(user);
        return entityManager.persistAndFlush(tenant);
    }

    @Test
    @DisplayName("deleteApplicationsByListingId native query deletes only target listing applications")
    void deleteApplicationsByListingId_DeletesTargetJoinRows() {
        User u1 = createUser("tenant1", "tenant1@test.com");
        User u2 = createUser("tenant2", "tenant2@test.com");

        Listing listingA = createListing("Apartment A", "Street A 1");
        Listing listingB = createListing("Apartment B", "Street B 2");

        Tenant tenant1 = createTenant(u1, "Alice", "Smith", "1111111111");
        Tenant tenant2 = createTenant(u2, "Bob", "Jones", "2222222222");

        tenant1.getAppliedListings().add(listingA);
        tenant1.getAppliedListings().add(listingB);
        tenant2.getAppliedListings().add(listingA);

        entityManager.persistAndFlush(tenant1);
        entityManager.persistAndFlush(tenant2);
        entityManager.clear();

        // Native SQL delete for listingA
        tenantRepository.deleteApplicationsByListingId(listingA.getId());
        entityManager.clear();

        Number countListingA = (Number) entityManager.getEntityManager()
                .createNativeQuery("SELECT count(*) FROM tenant_listing_applications WHERE listing_id = :id")
                .setParameter("id", listingA.getId())
                .getSingleResult();

        Number countListingB = (Number) entityManager.getEntityManager()
                .createNativeQuery("SELECT count(*) FROM tenant_listing_applications WHERE listing_id = :id")
                .setParameter("id", listingB.getId())
                .getSingleResult();

        assertThat(countListingA.intValue()).isEqualTo(0);
        assertThat(countListingB.intValue()).isEqualTo(1);

        // Ensure listings and tenants still exist
        assertThat(entityManager.find(Listing.class, listingA.getId())).isNotNull();
        assertThat(entityManager.find(Tenant.class, tenant1.getId())).isNotNull();
    }

    @Test
    @DisplayName("findByUserId returns associated Tenant")
    void findByUserId_ReturnsAssociatedTenant() {
        User user = createUser("tenantUser", "tenantUser@test.com");
        Tenant tenant = createTenant(user, "John", "Doe", "3333333333");

        Optional<Tenant> found = tenantRepository.findByUserId(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(tenant.getId());
        assertThat(found.get().getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("findByUserId with non-existent user returns empty Optional")
    void findByUserId_NonExistentUser_ReturnsEmpty() {
        Optional<Tenant> found = tenantRepository.findByUserId(99999);
        assertThat(found).isEmpty();
    }
}
