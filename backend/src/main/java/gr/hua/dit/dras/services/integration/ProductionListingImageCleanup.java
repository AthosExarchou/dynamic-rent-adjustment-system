package gr.hua.dit.dras.services.integration;

/* imports */
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.repositories.ListingRepository;
import gr.hua.dit.dras.services.domain.ListingImagePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

/**
 * Removes image references at startup that production is not allowed to keep.
 */
@Component
@ConditionalOnProperty(name = "dras.external-images.enabled", havingValue = "false", matchIfMissing = false)
public class ProductionListingImageCleanup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductionListingImageCleanup.class);
    private static final int BATCH_SIZE = 500;

    private final ListingRepository listingRepository;
    private final ListingImagePolicy imagePolicy;

    public ProductionListingImageCleanup(ListingRepository listingRepository, ListingImagePolicy imagePolicy) {
        this.listingRepository = listingRepository;
        this.imagePolicy = imagePolicy;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int pageNumber = 0;
        int totalChanged = 0;

        while (true) {
            Page<Listing> page = listingRepository.findAll(
                    PageRequest.of(pageNumber, BATCH_SIZE, Sort.by("id")));
            List<Listing> changed = new ArrayList<>();

            for (Listing listing : page) {
                List<String> current = listing.getImages();
                List<String> cleaned = imagePolicy.forListing(listing);
                if (current == null || !cleaned.equals(current)) {
                    listing.setImages(new ArrayList<>(cleaned));
                    changed.add(listing);
                }
            }

            if (!changed.isEmpty()) {
                listingRepository.saveAll(changed);
                totalChanged += changed.size();
            }

            if (!page.hasNext()) {
                break;
            }
            pageNumber++;
        }

        if (totalChanged > 0) {
            log.info("Production image cleanup removed unsupported image references from {} listings", totalChanged);
        }
    }
}
