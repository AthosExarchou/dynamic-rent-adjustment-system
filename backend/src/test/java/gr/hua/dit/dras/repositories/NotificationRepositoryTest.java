package gr.hua.dit.dras.repositories;

/* imports */
import gr.hua.dit.dras.entities.Notification;
import gr.hua.dit.dras.entities.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class NotificationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NotificationRepository notificationRepository;

    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("ValidPassword123!");
        return entityManager.persistAndFlush(user);
    }

    private Notification createNotification(User user, String title, boolean isRead, LocalDateTime createdAt) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage("Test message for " + title);
        notification.setIsRead(isRead);
        notification.setCreatedAt(createdAt);
        return entityManager.persistAndFlush(notification);
    }

    @Test
    @DisplayName("markAllReadForUser updates only target user unread notifications")
    void markAllReadForUser_UpdatesOnlyTargetUserUnread() {
        User userA = createUser("userA", "userA@test.com");
        User userB = createUser("userB", "userB@test.com");

        Notification n1 = createNotification(userA, "N1", false, LocalDateTime.now().minusHours(3));
        Notification n2 = createNotification(userA, "N2", false, LocalDateTime.now().minusHours(2));
        Notification n3 = createNotification(userA, "N3", true, LocalDateTime.now().minusHours(1));
        Notification nOther = createNotification(userB, "N4", false, LocalDateTime.now());

        entityManager.clear();

        notificationRepository.markAllReadForUser(userA.getId());
        entityManager.clear();

        Notification updatedN1 = entityManager.find(Notification.class, n1.getId());
        Notification updatedN2 = entityManager.find(Notification.class, n2.getId());
        Notification updatedN3 = entityManager.find(Notification.class, n3.getId());
        Notification untouchedOther = entityManager.find(Notification.class, nOther.getId());

        assertThat(updatedN1.getIsRead()).isTrue();
        assertThat(updatedN2.getIsRead()).isTrue();
        assertThat(updatedN3.getIsRead()).isTrue();
        assertThat(untouchedOther.getIsRead()).isFalse();
    }

    @Test
    @DisplayName("deleteAllByUserId deletes only target user notifications")
    void deleteAllByUserId_DeletesOnlyTargetUserNotifications() {
        User userA = createUser("userA", "userA@test.com");
        User userB = createUser("userB", "userB@test.com");

        Notification n1 = createNotification(userA, "N1", false, LocalDateTime.now());
        Notification n2 = createNotification(userB, "N2", false, LocalDateTime.now());

        entityManager.clear();

        notificationRepository.deleteAllByUserId(userA.getId());
        entityManager.clear();

        assertThat(entityManager.find(Notification.class, n1.getId())).isNull();
        assertThat(entityManager.find(Notification.class, n2.getId())).isNotNull();
    }

    @Test
    @DisplayName("findByUser_IdOrderByCreatedAtDesc returns notifications ordered newest first")
    void findByUser_IdOrderByCreatedAtDesc_ReturnsOrdered() {
        User user = createUser("userSort", "userSort@test.com");

        LocalDateTime now = LocalDateTime.now();
        Notification oldest = createNotification(user, "Oldest", false, now.minusHours(2));
        Notification newest = createNotification(user, "Newest", false, now);
        Notification middle = createNotification(user, "Middle", false, now.minusHours(1));

        List<Notification> results = notificationRepository.findByUser_IdOrderByCreatedAtDesc(user.getId());

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getId()).isEqualTo(newest.getId());
        assertThat(results.get(1).getId()).isEqualTo(middle.getId());
        assertThat(results.get(2).getId()).isEqualTo(oldest.getId());
    }

    @Test
    @DisplayName("countByUser_IdAndIsReadFalse returns accurate count of unread notifications")
    void countByUser_IdAndIsReadFalse_CountsAccurately() {
        User user = createUser("userCount", "userCount@test.com");

        createNotification(user, "N1", false, LocalDateTime.now());
        createNotification(user, "N2", false, LocalDateTime.now());
        createNotification(user, "N3", true, LocalDateTime.now());

        Long unreadCount = notificationRepository.countByUser_IdAndIsReadFalse(user.getId());

        assertThat(unreadCount).isEqualTo(2L);
    }
}
