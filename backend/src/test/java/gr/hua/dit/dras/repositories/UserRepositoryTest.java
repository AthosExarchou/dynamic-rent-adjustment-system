package gr.hua.dit.dras.repositories;

import gr.hua.dit.dras.entities.Notification;
import gr.hua.dit.dras.entities.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User buildUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("ValidPassword123!");
        return user;
    }

    @Test
    @DisplayName("findByUsername returns user when present")
    void findByUsername_WhenPresent_ReturnsUser() {
        User user = entityManager.persistAndFlush(buildUser("johnny", "johnny@test.com"));

        Optional<User> found = userRepository.findByUsername("johnny");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(user.getId());
        assertThat(found.get().getEmail()).isEqualTo("johnny@test.com");
    }

    @Test
    @DisplayName("findByEmail returns user when present")
    void findByEmail_WhenPresent_ReturnsUser() {
        User user = entityManager.persistAndFlush(buildUser("clara", "clara@test.com"));

        Optional<User> found = userRepository.findByEmail("clara@test.com");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(user.getId());
        assertThat(found.get().getUsername()).isEqualTo("clara");
    }

    @Test
    @DisplayName("Saving duplicate username violates DB unique constraint")
    void saveUser_DuplicateUsername_ThrowsDataIntegrityViolation() {
        entityManager.persistAndFlush(buildUser("unique_user", "user1@test.com"));

        User duplicateUser = buildUser("unique_user", "user2@test.com");

        assertThatThrownBy(() -> {
            userRepository.saveAndFlush(duplicateUser);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Saving duplicate email violates DB unique constraint")
    void saveUser_DuplicateEmail_ThrowsDataIntegrityViolation() {
        entityManager.persistAndFlush(buildUser("user_a", "same_email@test.com"));

        User duplicateUser = buildUser("user_b", "same_email@test.com");

        assertThatThrownBy(() -> {
            userRepository.saveAndFlush(duplicateUser);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Deleting user cascades deletion to notifications")
    void deleteUser_CascadesToNotifications() {
        User user = entityManager.persistAndFlush(buildUser("cascadeUser", "cascade@test.com"));

        Notification n = new Notification();
        n.setUser(user);
        n.setTitle("Cascade Test");
        n.setMessage("Notification message");
        n.setCreatedAt(LocalDateTime.now());
        Notification savedNotification = entityManager.persistAndFlush(n);

        Integer notificationId = savedNotification.getId();
        Integer userId = user.getId();

        entityManager.clear();

        User managedUser = entityManager.find(User.class, userId);
        userRepository.delete(managedUser);
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(User.class, userId)).isNull();
        assertThat(entityManager.find(Notification.class, notificationId)).isNull();
    }
}
