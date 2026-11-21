package gr.hua.dit.dras.repositories;

/* imports */
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.model.enums.ListingStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Integer>, JpaSpecificationExecutor<Listing> {

    List<Listing> findByOwner(Owner owner);
    Optional<Listing> findBySourceUrl(String sourceUrl);
    List<Listing> findByExternalFalse();
    List<Listing> findByExternalTrue();
    List<Listing> findByStatus(ListingStatus status);
    long countByExternalTrue();
    long countByExternalFalse();
    long countByStatus(ListingStatus status);
    @Query("""
            select l.id from Listing l
            where l.external = true and l.dateScraped < :cutoff
            order by l.id
            """)
    List<Integer> findStaleExternalListingIds(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Query("select count(l) from Listing l where l.external = true and l.dateScraped < :cutoff")
    long countStaleExternalListings(@Param("cutoff") Instant cutoff);

    @EntityGraph(attributePaths = {"applications", "tenant"})
    @Query("select l from Listing l where l.id in :ids")
    List<Listing> findAllWithRelationsByIdIn(@Param("ids") Collection<Integer> ids);

    @Modifying
    @Query(value = "DELETE FROM user_favourites WHERE listing_id = :listingId", nativeQuery = true)
    void deleteFavouritesByListingId(@Param("listingId") Integer listingId);

    @Modifying
    @Query(value = "DELETE FROM user_favourites WHERE listing_id IN (:listingIds)", nativeQuery = true)
    void deleteFavouritesByListingIds(@Param("listingIds") Collection<Integer> listingIds);

}
