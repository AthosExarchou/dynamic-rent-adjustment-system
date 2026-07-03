package gr.hua.dit.dras.services.domain;

import gr.hua.dit.dras.dto.NotificationDTO;
import gr.hua.dit.dras.entities.Notification;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.repositories.NotificationRepository;
import gr.hua.dit.dras.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void createNotification(User user, String title, String message) {
        if (user == null) return;
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notificationRepository.save(notification);
    }

    @Transactional
    public void createNotificationByEmail(String email, String title, String message) {
        userRepository.findByEmail(email).ifPresent(user -> {
            createNotification(user, title, message);
        });
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getUserNotifications(Integer userId) {
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Long getUnreadCount(Integer userId) {
        return notificationRepository.countByUser_IdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Integer notificationId, Integer userId) {
        notificationRepository.findById(notificationId).ifPresent(notif -> {
            if (notif.getUser().getId().equals(userId)) {
                notif.setIsRead(true);
                notificationRepository.save(notif);
            }
        });
    }

    @Transactional
    public void markAllAsRead(Integer userId) {
        notificationRepository.markAllReadForUser(userId);
    }
}
