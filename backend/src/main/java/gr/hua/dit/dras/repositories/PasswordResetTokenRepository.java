package gr.hua.dit.dras.repositories;

/* imports */
import gr.hua.dit.dras.entities.PasswordResetToken;
import gr.hua.dit.dras.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByUser(User user);

}
