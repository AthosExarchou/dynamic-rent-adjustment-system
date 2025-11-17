package gr.hua.dit.dras.repositories;

/* imports */
import gr.hua.dit.dras.entities.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Integer> {
    Optional<Tenant> findByUserId(Integer userId);

    @Modifying
    @Query(value = "DELETE FROM tenant_listing_applications WHERE listing_id = :listingId", nativeQuery = true)
    void deleteApplicationsByListingId(@Param("listingId") Integer listingId);
}
