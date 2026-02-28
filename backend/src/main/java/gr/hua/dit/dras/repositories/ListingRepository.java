package gr.hua.dit.dras.repositories;

/* imports */
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.model.enums.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Integer> {

    List<Listing> findByOwner(Owner owner);
    List<Listing> findByPriceBetween(int minPrice, int maxPrice);
    List<Listing> findByTitleContainingIgnoreCaseAndPriceBetween(String title, int minPrice, int maxPrice);
    Optional<Listing> findBySourceUrl(String sourceUrl);
    List<Listing> findByExternalFalse();
    List<Listing> findByExternalTrue();
    List<Listing> findByStatus(ListingStatus status);

}
