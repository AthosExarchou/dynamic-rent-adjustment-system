package gr.hua.dit.dras.repositories;

/* imports */
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.ListingApplication;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ListingApplicationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ListingApplicationRepository listingApplicationRepository;

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
    @DisplayName("Should save and find ListingApplication by ID")
    void saveAndFindById() {
        User user = createUser("tenant1", "tenant1@test.com");
        Tenant tenant = createTenant(user, "Alice", "Smith", "1111111111");
        Listing listing = createListing("Apartment A", "Street A 1");

        ListingApplication application = new ListingApplication(tenant, listing, "Quiet tenant");
        ListingApplication saved = listingApplicationRepository.save(application);

        Optional<ListingApplication> found = listingApplicationRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getNotes()).isEqualTo("Quiet tenant");
        assertThat(found.get().getTenant().getId()).isEqualTo(tenant.getId());
        assertThat(found.get().getListing().getId()).isEqualTo(listing.getId());
    }

    @Test
    @DisplayName("Should find application by listing ID and tenant ID")
    void findByListingIdAndTenantId() {
        User user = createUser("tenant2", "tenant2@test.com");
        Tenant tenant = createTenant(user, "Bob", "Jones", "2222222222");
        Listing listing = createListing("Apartment B", "Street B 2");

        ListingApplication application = new ListingApplication(tenant, listing, "First application");
        listingApplicationRepository.save(application);

        Optional<ListingApplication> found = listingApplicationRepository.findByListingIdAndTenantId(listing.getId(), tenant.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getNotes()).isEqualTo("First application");

        Optional<ListingApplication> notFound = listingApplicationRepository.findByListingIdAndTenantId(listing.getId(), 9999);
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("Should check exists by listing ID and tenant ID")
    void existsByListingIdAndTenantId() {
        User user = createUser("tenant3", "tenant3@test.com");
        Tenant tenant = createTenant(user, "Charlie", "Brown", "3333333333");
        Listing listing = createListing("Apartment C", "Street C 3");

        assertThat(listingApplicationRepository.existsByListingIdAndTenantId(listing.getId(), tenant.getId())).isFalse();

        listingApplicationRepository.save(new ListingApplication(tenant, listing));

        assertThat(listingApplicationRepository.existsByListingIdAndTenantId(listing.getId(), tenant.getId())).isTrue();
    }

    @Test
    @DisplayName("Should find all applications for a listing ordered by appliedAt descending")
    void findByListingIdOrderByAppliedAtDesc() {
        User u1 = createUser("tenant4", "tenant4@test.com");
        User u2 = createUser("tenant5", "tenant5@test.com");
        Tenant t1 = createTenant(u1, "David", "Miller", "4444444444");
        Tenant t2 = createTenant(u2, "Eva", "Green", "5555555555");
        Listing listing = createListing("Apartment D", "Street D 4");

        listingApplicationRepository.save(new ListingApplication(t1, listing, "First note"));
        listingApplicationRepository.save(new ListingApplication(t2, listing, "Second note"));

        List<ListingApplication> apps = listingApplicationRepository.findByListingIdOrderByAppliedAtDesc(listing.getId());
        assertThat(apps).hasSize(2);
    }

    @Test
    @DisplayName("Should find all applications for a tenant")
    void findByTenantId() {
        User user = createUser("tenant6", "tenant6@test.com");
        Tenant tenant = createTenant(user, "Frank", "Castle", "6666666666");
        Listing l1 = createListing("Flat 1", "Address 1");
        Listing l2 = createListing("Flat 2", "Address 2");

        listingApplicationRepository.save(new ListingApplication(tenant, l1));
        listingApplicationRepository.save(new ListingApplication(tenant, l2));

        List<ListingApplication> apps = listingApplicationRepository.findByTenantId(tenant.getId());
        assertThat(apps).hasSize(2);
    }

    @Test
    @DisplayName("Should delete applications by listing ID")
    void deleteByListingId() {
        User user = createUser("tenant7", "tenant7@test.com");
        Tenant tenant = createTenant(user, "Grace", "Hopper", "7777777777");
        Listing listing = createListing("Flat 3", "Address 3");

        listingApplicationRepository.save(new ListingApplication(tenant, listing));
        assertThat(listingApplicationRepository.findByListingId(listing.getId())).hasSize(1);

        listingApplicationRepository.deleteByListingId(listing.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(listingApplicationRepository.findByListingId(listing.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should delete application by listing ID and tenant ID")
    void deleteByListingIdAndTenantId() {
        User user = createUser("tenant8", "tenant8@test.com");
        Tenant tenant = createTenant(user, "Hank", "Pym", "8888888888");
        Listing listing = createListing("Flat 4", "Address 4");

        listingApplicationRepository.save(new ListingApplication(tenant, listing));
        assertThat(listingApplicationRepository.existsByListingIdAndTenantId(listing.getId(), tenant.getId())).isTrue();

        listingApplicationRepository.deleteByListingIdAndTenantId(listing.getId(), tenant.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(listingApplicationRepository.existsByListingIdAndTenantId(listing.getId(), tenant.getId())).isFalse();
    }

    @Test
    @DisplayName("Should delete applications by tenant ID")
    void deleteByTenantId() {
        User user = createUser("tenant9", "tenant9@test.com");
        Tenant tenant = createTenant(user, "Iris", "West", "9999999999");
        Listing listing = createListing("Flat 5", "Address 5");

        listingApplicationRepository.save(new ListingApplication(tenant, listing));
        assertThat(listingApplicationRepository.findByTenantId(tenant.getId())).hasSize(1);

        listingApplicationRepository.deleteByTenantId(tenant.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(listingApplicationRepository.findByTenantId(tenant.getId())).isEmpty();
    }
}
