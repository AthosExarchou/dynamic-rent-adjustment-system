package gr.hua.dit.dras.repositories;

/* imports */
import gr.hua.dit.dras.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String name);

    /* Counts real platform accounts: excludes administrators and the
       synthetic user that owns externally imported listings. */
    @Query("""
            select count(distinct u)
            from User u
            where not exists (
                select admin.id
                from User admin
                join admin.roles role
                where admin.id = u.id and role.name = 'ADMIN'
            )
            and not exists (
                select owner.id
                from Owner owner
                where owner.user = u and owner.systemOwner = true
            )
            """)
    long countPlatformUsers();

}
