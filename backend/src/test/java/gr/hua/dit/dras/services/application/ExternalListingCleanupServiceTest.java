package gr.hua.dit.dras.services.application;

/* imports */
import gr.hua.dit.dras.dto.ExternalCleanupResult;
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.ListingApplication;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.entities.Tenant;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.model.enums.PropertyType;
import gr.hua.dit.dras.model.enums.RentalDuration;
import gr.hua.dit.dras.model.enums.RentalStatus;
import gr.hua.dit.dras.repositories.ListingApplicationRepository;
import gr.hua.dit.dras.repositories.ListingRepository;
import gr.hua.dit.dras.repositories.TenantRepository;
import gr.hua.dit.dras.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ExternalListingCleanupServiceTest {

    @Autowired
    private ExternalListingCleanupService cleanupService;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ListingApplicationRepository listingApplicationRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setPassword("ValidPassword123!");
        return userRepository.save(user);
    }

    private Listing createListing(String title, boolean external, Instant scraped) {
        Listing listing = new Listing();
        listing.setTitle(title);
        listing.setDescription("Description for " + title);
        listing.setPrice(500);
        listing.setPricePerM2(10);
        listing.setAddress("Athens Center 1");
        listing.setSizeM2(50);
        listing.setPropertyType(PropertyType.APARTMENT);
        listing.setRentalDuration(RentalDuration.LONG_TERM);
        listing.setStatus(ListingStatus.APPROVED);
        listing.setExternal(external);
        listing.setSourceUrl(external ? "https://example.com/" + title : null);
        listing.setDateScraped(scraped);
        return listingRepository.save(listing);
    }

    @Test
    @DisplayName("Removes stale external listings with their favourites and applications")
    void cleanupDeletesStaleExternalListingsWithAssociations() {
        User ownerUser = createUser("cleanupOwner");
        Owner owner = new Owner("Owner", "User", "1000000001", false);
        owner.setUser(ownerUser);
        entityManager.persist(owner);

        User tenantUser = createUser("cleanupTenant");
        Tenant tenant = new Tenant("Tenant", "User", "1000000002");
        tenant.setUser(tenantUser);
        tenantRepository.save(tenant);

        Instant now = Instant.now();
        Listing stale = createListing("Stale", true, now.minus(40, ChronoUnit.DAYS));
        stale.setOwner(owner);
        Listing fresh = createListing("Fresh", true, now.minus(5, ChronoUnit.DAYS));
        Listing localOld = createListing("LocalOld", false, now.minus(40, ChronoUnit.DAYS));

        User favouriteUser = createUser("cleanupFavourite");
        favouriteUser.addFavouriteListing(stale);
        userRepository.save(favouriteUser);

        ListingApplication application = tenant.applyToListing(stale, null);
        listingApplicationRepository.save(application);
        listingRepository.save(stale);
        tenantRepository.save(tenant);
        entityManager.flush();

        ExternalCleanupResult result = cleanupService.cleanup(30, false);
        entityManager.flush();

        assertThat(result.deleted()).isEqualTo(1);
        assertThat(listingRepository.findById(stale.getId())).isEmpty();
        assertThat(listingRepository.findById(fresh.getId())).isPresent();
        assertThat(listingRepository.findById(localOld.getId())).isPresent();
        assertThat(listingApplicationRepository.findById(application.getId())).isEmpty();

        entityManager.clear();
        assertThat(userRepository.findById(favouriteUser.getId()).orElseThrow()
                .getFavouriteListings()).isEmpty();
        assertThat(tenantRepository.findById(tenant.getId()).orElseThrow()
                .getRentalStatus()).isEqualTo(RentalStatus.CANCELED);

        ExternalCleanupResult secondRun = cleanupService.cleanup(30, false);
        entityManager.flush();
        assertThat(secondRun.deleted()).isZero();
    }

    @Test
    @DisplayName("Rejects a non-positive grace period before acquiring the lock")
    void cleanupRejectsNonPositiveGraceDays() {
        assertThatThrownBy(() -> cleanupService.cleanup(0, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("graceDays must be greater than zero");
    }

    @Test
    @DisplayName("Dry run reports matches without deleting anything")
    void dryRunDoesNotDelete() {
        Listing stale = createListing("DryRunStale", true,
                Instant.now().minus(40, ChronoUnit.DAYS));
        entityManager.flush();

        ExternalCleanupResult result = cleanupService.cleanup(30, true);

        assertThat(result.matched()).isGreaterThanOrEqualTo(1);
        assertThat(result.deleted()).isZero();
        assertThat(listingRepository.findById(stale.getId())).isPresent();
    }
}
