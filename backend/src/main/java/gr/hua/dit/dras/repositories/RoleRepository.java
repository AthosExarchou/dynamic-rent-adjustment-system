package gr.hua.dit.dras.repositories;

/* imports */
import gr.hua.dit.dras.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByName(String roleName);

    @Transactional(isolation = Isolation.SERIALIZABLE)
    default Role updateOrInsert(Role role) {
        Role existing_role = findByName(role.getName()).orElse(null);
        if (existing_role != null) {
            return existing_role;
        }
        else {
            return save(role);
        }
    }
}
