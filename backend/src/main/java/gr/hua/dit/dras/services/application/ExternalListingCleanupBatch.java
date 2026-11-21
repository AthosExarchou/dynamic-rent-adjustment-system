package gr.hua.dit.dras.services.application;

/* imports */
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.ListingApplication;
import gr.hua.dit.dras.entities.Tenant;
import gr.hua.dit.dras.repositories.ListingApplicationRepository;
import gr.hua.dit.dras.repositories.ListingRepository;
import gr.hua.dit.dras.repositories.TenantRepository;
import gr.hua.dit.dras.services.domain.TenantService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Deletes one bounded batch of stale external listings.
 * Kept separate from the scheduler so each batch gets its own transaction.
 */
@Component
public class ExternalListingCleanupBatch {

    private final ListingRepository listingRepository;
    private final ListingApplicationRepository listingApplicationRepository;
    private final TenantRepository tenantRepository;
    private final TenantService tenantService;

    public ExternalListingCleanupBatch(
            ListingRepository listingRepository,
            ListingApplicationRepository listingApplicationRepository,
            TenantRepository tenantRepository,
            TenantService tenantService
    ) {
        this.listingRepository = listingRepository;
        this.listingApplicationRepository = listingApplicationRepository;
        this.tenantRepository = tenantRepository;
        this.tenantService = tenantService;
    }

    @Transactional
    public int cleanupBatch(Instant cutoff, int batchSize, boolean dryRun) {
        List<Integer> listingIds = listingRepository
                .findStaleExternalListingIds(cutoff, PageRequest.of(0, batchSize));

        if (listingIds.isEmpty()) {
            return 0;
        }
        if (dryRun) {
            return listingIds.size();
        }

        List<Listing> listings = listingRepository.findAllWithRelationsByIdIn(listingIds);

        for (ListingApplication application : listingApplicationRepository.findByListingIdIn(listingIds)) {
            Tenant tenant = application.getTenant();
            if (tenant != null) {
                tenant.processRejection(application.getListing());
                tenantRepository.save(tenant);
            }
        }
        listingApplicationRepository.deleteByListingIdIn(listingIds);

        for (Listing listing : listings) {
            if (listing.getTenant() != null) {
                tenantService.unassignTenantFromListing(listing.getId(), listing.getTenant().getId());
            }
        }

        listingRepository.deleteFavouritesByListingIds(listingIds);
        listingRepository.deleteAll(listings);

        return listings.size();
    }
}
