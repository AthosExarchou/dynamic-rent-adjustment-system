package gr.hua.dit.dras.repositories;

import gr.hua.dit.dras.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findByUser_IdOrderByCreatedAtDesc(Integer userId);
    Long countByUser_IdAndIsReadFalse(Integer userId);

    @Transactional
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllReadForUser(@Param("userId") Integer userId);
}

