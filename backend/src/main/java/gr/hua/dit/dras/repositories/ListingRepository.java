package gr.hua.dit.dras.repositories;

/* imports */
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Integer> {

    List<Listing> findByOwner(Owner owner);
    List<Listing> findByPriceBetween(int minPrice, int maxPrice);
    List<Listing> findByTitleContainingIgnoreCaseAndPriceBetween(String title, int minPrice, int maxPrice);

}
