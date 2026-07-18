package com.koinonia.backend.notification;

import com.koinonia.backend.comment.Comment;
import com.koinonia.backend.post.Post;
import com.koinonia.backend.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    List<Notification> findByRecipientIdAndReadAtIsNull(Long recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = CURRENT_TIMESTAMP WHERE n.recipient.id = :recipientId AND n.readAt IS NULL")
    long markAllAsReadForRecipient(@Param("recipientId") Long recipientId);

    @Query("SELECT COUNT(n) > 0 FROM Notification n WHERE n.recipient = :recipient AND n.actor = :actor AND n.post = :post AND n.type = :type AND n.createdAt > :cutoff")
    boolean existsRecentNotification(
            @Param("recipient") User recipient,
            @Param("actor") User actor,
            @Param("post") Post post,
            @Param("type") NotificationType type,
            @Param("cutoff") LocalDateTime cutoff
    );

    @Query("SELECT COUNT(n) > 0 FROM Notification n WHERE n.recipient = :recipient AND n.actor = :actor AND n.comment = :comment AND n.type = :type AND n.createdAt > :cutoff")
    boolean existsRecentNotificationForComment(
            @Param("recipient") User recipient,
            @Param("actor") User actor,
            @Param("comment") Comment comment,
            @Param("type") NotificationType type,
            @Param("cutoff") LocalDateTime cutoff
    );

    void deleteAllByRecipientId(Long recipientId);
}

