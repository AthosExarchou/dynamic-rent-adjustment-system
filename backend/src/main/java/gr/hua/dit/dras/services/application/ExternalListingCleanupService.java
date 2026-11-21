package gr.hua.dit.dras.services.application;

/* imports */
import gr.hua.dit.dras.dto.ExternalCleanupResult;
import gr.hua.dit.dras.repositories.ListingRepository;
import gr.hua.dit.dras.services.infrastructure.CleanupLock;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Scheduled maintenance for external listings that are not in the scrapes.
 */
@Service
public class ExternalListingCleanupService {

    private static final Logger log = LoggerFactory.getLogger(ExternalListingCleanupService.class);

    private final ExternalListingCleanupBatch cleanupBatch;
    private final ListingRepository listingRepository;
    private final CleanupLock cleanupLock;

    @Value("${app.cleanup.grace-days:30}")
    private int graceDays;

    @Value("${app.cleanup.batch-size:200}")
    private int batchSize;

    public ExternalListingCleanupService(
            ExternalListingCleanupBatch cleanupBatch,
            ListingRepository listingRepository,
            CleanupLock cleanupLock
    ) {
        this.cleanupBatch = cleanupBatch;
        this.listingRepository = listingRepository;
        this.cleanupLock = cleanupLock;
    }

    @PostConstruct
    void validateConfiguration() {
        if (graceDays <= 0) {
            throw new IllegalStateException("app.cleanup.grace-days must be greater than zero");
        }
        if (batchSize <= 0) {
            throw new IllegalStateException("app.cleanup.batch-size must be greater than zero");
        }
    }

    @Scheduled(cron = "${app.cleanup.cron:0 30 3 * * *}")
    public void scheduledCleanup() {
        ExternalCleanupResult result = cleanup(graceDays, false);
        if (!result.lockAcquired()) {
            return;
        }
        log.info("Scheduled external listing cleanup finished | matched={} deleted={} durationMs={}",
                result.matched(), result.deleted(), result.durationMillis());
    }

    public ExternalCleanupResult cleanup(int days, boolean dryRun) {
        if (days <= 0) {
            throw new IllegalArgumentException("graceDays must be greater than zero");
        }

        long started = System.currentTimeMillis();
        if (!cleanupLock.tryLock()) {
            return new ExternalCleanupResult(0, 0, dryRun, days,
                    System.currentTimeMillis() - started, false);
        }

        int matched = 0;
        int deleted = 0;
        try {
            Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);

            if (dryRun) {
                matched = (int) listingRepository.countStaleExternalListings(cutoff);
            } else {
                for (;;) {
                    int count = cleanupBatch.cleanupBatch(cutoff, batchSize, false);
                    if (count == 0) {
                        break;
                    }
                    deleted += count;
                    if (count < batchSize) {
                        break;
                    }
                }
                matched = deleted;
            }
        } finally {
            cleanupLock.unlock();
        }

        long duration = System.currentTimeMillis() - started;
        log.info("External listing cleanup | matched={} deleted={} dryRun={} graceDays={} durationMs={}",
                matched, deleted, dryRun, days, duration);
        return new ExternalCleanupResult(matched, deleted, dryRun, days, duration, true);
    }
}
