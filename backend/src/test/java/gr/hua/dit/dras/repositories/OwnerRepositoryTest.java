package gr.hua.dit.dras.repositories;

/* imports */
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.entities.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OwnerRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OwnerRepository ownerRepository;

    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("ValidPassword123!");
        return entityManager.persistAndFlush(user);
    }

    private Owner createOwner(User user, String firstName, String lastName, String phone, boolean isSystemOwner) {
        Owner owner = new Owner(firstName, lastName, phone, isSystemOwner);
        owner.setUser(user);
        return entityManager.persistAndFlush(owner);
    }

    @Test
    @DisplayName("findByUserId returns associated owner entity")
    void findByUserId_ReturnsAssociatedOwner() {
        User user = createUser("owner_user", "owner@test.com");
        Owner owner = createOwner(user, "John", "Smith", "6900000001", false);

        Optional<Owner> found = ownerRepository.findByUserId(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(owner.getId());
        assertThat(found.get().getFirstName()).isEqualTo("John");
        assertThat(found.get().isSystemOwner()).isFalse();
    }

    @Test
    @DisplayName("findByUserId with non-existent user ID returns empty Optional")
    void findByUserId_NonExistentUser_ReturnsEmpty() {
        Optional<Owner> found = ownerRepository.findByUserId(99999);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findBySystemOwnerTrue locates designated system owner")
    void findBySystemOwnerTrue_ReturnsSystemOwner() {
        User systemUser = createUser("sys_user", "sys@test.com");
        Owner systemOwner = createOwner(systemUser, "System", "Owner", "0000000000", true);

        User normalUser = createUser("regular_user", "regular@test.com");
        createOwner(normalUser, "Regular", "Owner", "6900000002", false);

        Optional<Owner> found = ownerRepository.findBySystemOwnerTrue();

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(systemOwner.getId());
        assertThat(found.get().isSystemOwner()).isTrue();
    }

    @Test
    @DisplayName("findBySystemOwnerTrue returns empty when no system owner is present")
    void findBySystemOwnerTrue_WhenNoneExists_ReturnsEmpty() {
        User normalUser = createUser("regular_user2", "regular2@test.com");
        createOwner(normalUser, "Regular", "Owner", "6900000003", false);

        Optional<Owner> found = ownerRepository.findBySystemOwnerTrue();

        assertThat(found).isEmpty();
    }
}
