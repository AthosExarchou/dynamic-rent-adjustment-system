package gr.hua.dit.dras.repositories;

/* imports */
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.model.enums.PropertyType;
import gr.hua.dit.dras.model.enums.RentalDuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ListingRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ListingRepository listingRepository;

    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("ValidPassword123!");
        return entityManager.persistAndFlush(user);
    }

    private Owner createOwner(User user, String phone) {
        Owner owner = new Owner("OwnerFirst", "OwnerLast", phone, false);
        owner.setUser(user);
        return entityManager.persistAndFlush(owner);
    }

    private Listing createListing(Owner owner, String title, boolean external, String sourceUrl, ListingStatus status, Instant scraped) {
        Listing listing = new Listing();
        listing.setTitle(title);
        listing.setDescription("Description for " + title);
        listing.setPrice(500);
        listing.setPricePerM2(10);
        listing.setAddress("Athens Center 1");
        listing.setSizeM2(50);
        listing.setPropertyType(PropertyType.APARTMENT);
        listing.setRentalDuration(RentalDuration.LONG_TERM);
        listing.setStatus(status);
        listing.setExternal(external);
        listing.setSourceUrl(sourceUrl);
        listing.setOwner(owner);
        listing.setDateScraped(scraped);
        return entityManager.persistAndFlush(listing);
    }

    @Test
    @DisplayName("findByOwner returns only listings belonging to specified owner")
    void findByOwner_ReturnsOnlyOwnerListings() {
        User u1 = createUser("ownerUser1", "owner1@test.com");
        User u2 = createUser("ownerUser2", "owner2@test.com");
        Owner owner1 = createOwner(u1, "1000000001");
        Owner owner2 = createOwner(u2, "1000000002");

        Listing l1 = createListing(owner1, "Listing 1", false, null, ListingStatus.APPROVED, null);
        Listing l2 = createListing(owner1, "Listing 2", false, null, ListingStatus.APPROVED, null);
        Listing l3 = createListing(owner2, "Listing 3", false, null, ListingStatus.APPROVED, null);

        List<Listing> owner1Listings = listingRepository.findByOwner(owner1);

        assertThat(owner1Listings).hasSize(2)
                .extracting(Listing::getId)
                .containsExactlyInAnyOrder(l1.getId(), l2.getId());
    }

    @Test
    @DisplayName("findByExternalTrue and findByExternalFalse partition listings correctly")
    void findByExternal_PartitionsCorrectly() {
        User u = createUser("ownerExt", "ownerext@test.com");
        Owner owner = createOwner(u, "2000000001");

        createListing(owner, "Local Listing", false, null, ListingStatus.APPROVED, null);
        createListing(null, "Scraped Listing", true, "https://example.com/scraped1", ListingStatus.APPROVED, Instant.now());

        List<Listing> localListings = listingRepository.findByExternalFalse();
        List<Listing> externalListings = listingRepository.findByExternalTrue();

        assertThat(localListings).hasSize(1);
        assertThat(localListings.get(0).getTitle()).isEqualTo("Local Listing");

        assertThat(externalListings).hasSize(1);
        assertThat(externalListings.get(0).getTitle()).isEqualTo("Scraped Listing");
    }

    @Test
    @DisplayName("findBySourceUrl locates external listing")
    void findBySourceUrl_LocatesListing() {
        createListing(null, "External Item", true, "https://spitogatos.gr/item/999", ListingStatus.APPROVED, Instant.now());

        Optional<Listing> result = listingRepository.findBySourceUrl("https://spitogatos.gr/item/999");

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("External Item");
    }

    @Test
    @DisplayName("findByStatus returns listings with matching status")
    void findByStatus_ReturnsMatching() {
        createListing(null, "Approved 1", true, "https://example.com/item/1", ListingStatus.APPROVED, Instant.now());
        createListing(null, "Pending 1", false, null, ListingStatus.PENDING, null);

        List<Listing> approved = listingRepository.findByStatus(ListingStatus.APPROVED);
        List<Listing> pending = listingRepository.findByStatus(ListingStatus.PENDING);

        assertThat(approved).hasSize(1);
        assertThat(approved.get(0).getTitle()).isEqualTo("Approved 1");

        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getTitle()).isEqualTo("Pending 1");
    }

}
