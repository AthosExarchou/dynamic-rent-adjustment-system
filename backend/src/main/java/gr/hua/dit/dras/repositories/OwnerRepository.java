package gr.hua.dit.dras.repositories;

/* imports */
import gr.hua.dit.dras.entities.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OwnerRepository extends JpaRepository<Owner, Integer> {
    Optional<Owner> findByUserId(Integer userId);
}
