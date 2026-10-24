package gr.hua.dit.dras.repositories;

/* imports */
import gr.hua.dit.dras.entities.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RoleRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    @DisplayName("findByName returns role when matching name exists")
    void findByName_WhenExists_ReturnsRole() {
        Role role = new Role("MODERATOR");
        entityManager.persistAndFlush(role);

        Optional<Role> found = roleRepository.findByName("MODERATOR");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("MODERATOR");
    }

    @Test
    @DisplayName("findByName returns empty optional when role does not exist")
    void findByName_WhenNotExists_ReturnsEmpty() {
        Optional<Role> found = roleRepository.findByName("NON_EXISTENT_ROLE");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("updateOrInsert saves new role if not already in database")
    void updateOrInsert_NewRole_PersistsRole() {
        Role newRole = new Role("SUPPORT");

        Role result = roleRepository.updateOrInsert(newRole);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("SUPPORT");

        Optional<Role> inDb = roleRepository.findByName("SUPPORT");
        assertThat(inDb).isPresent();
    }

    @Test
    @DisplayName("updateOrInsert returns existing role without creating duplicate")
    void updateOrInsert_ExistingRole_ReturnsExistingWithoutDuplicate() {
        Role existingRole = new Role("AUDITOR");
        entityManager.persistAndFlush(existingRole);
        entityManager.clear();

        Role duplicateAttempt = new Role("AUDITOR");
        Role result = roleRepository.updateOrInsert(duplicateAttempt);

        assertThat(result.getId()).isEqualTo(existingRole.getId());
        assertThat(roleRepository.findAll().stream().filter(r -> r.getName().equals("AUDITOR")).count()).isEqualTo(1);
    }
}
