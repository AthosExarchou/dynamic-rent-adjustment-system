package gr.hua.dit.dras.repositories;

/* imports */
import gr.hua.dit.dras.entities.ListingApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListingApplicationRepository extends JpaRepository<ListingApplication, Integer> {

    Optional<ListingApplication> findByListingIdAndTenantId(Integer listingId, Integer tenantId);

    List<ListingApplication> findByListingId(Integer listingId);

    List<ListingApplication> findByListingIdOrderByAppliedAtDesc(Integer listingId);

    List<ListingApplication> findByTenantId(Integer tenantId);

    boolean existsByListingIdAndTenantId(Integer listingId, Integer tenantId);

    void deleteByListingId(Integer listingId);

    void deleteByTenantId(Integer tenantId);

    void deleteByListingIdAndTenantId(Integer listingId, Integer tenantId);
}
