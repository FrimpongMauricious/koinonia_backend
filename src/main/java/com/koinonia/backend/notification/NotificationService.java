package com.koinonia.backend.notification;

import com.koinonia.backend.comment.Comment;
import com.koinonia.backend.exception.ForbiddenException;
import com.koinonia.backend.exception.NotificationNotFoundException;
import com.koinonia.backend.notification.dto.NotificationResponse;
import com.koinonia.backend.post.Post;
import com.koinonia.backend.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Emit a LIKE notification when a user likes someone else's post.
     * Deduplicates within 24 hours.
     */
    @Transactional
    public void emitLike(User actor, User recipient, Post post) {
        if (actor.getId().equals(recipient.getId())) {
            return; // Self-like, skip
        }

        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        if (notificationRepository.existsRecentNotification(recipient, actor, post, NotificationType.LIKE, cutoff)) {
            return; // Already notified recently, skip
        }

        try {
            Notification notification = Notification.builder()
                    .recipient(recipient)
                    .actor(actor)
                    .type(NotificationType.LIKE)
                    .post(post)
                    .build();
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.warn("Failed to emit LIKE notification for actor {} to recipient {}: {}", actor.getId(), recipient.getId(), e.getMessage());
        }
    }

    /**
     * Emit a REPOST notification when a user reposts someone else's post.
     * Deduplicates within 24 hours.
     */
    @Transactional
    public void emitRepost(User actor, User recipient, Post post) {
        if (actor.getId().equals(recipient.getId())) {
            return; // Self-repost, skip
        }

        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        if (notificationRepository.existsRecentNotification(recipient, actor, post, NotificationType.REPOST, cutoff)) {
            return; // Already notified recently, skip
        }

        try {
            Notification notification = Notification.builder()
                    .recipient(recipient)
                    .actor(actor)
                    .type(NotificationType.REPOST)
                    .post(post)
                    .build();
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.warn("Failed to emit REPOST notification for actor {} to recipient {}: {}", actor.getId(), recipient.getId(), e.getMessage());
        }
    }

    /**
     * Emit a COMMENT notification when a user comments on someone else's post.
     * Always creates; no deduplication.
     */
    @Transactional
    public void emitComment(User actor, User recipient, Post post, Comment comment) {
        if (actor.getId().equals(recipient.getId())) {
            return; // Self-comment, skip
        }

        try {
            Notification notification = Notification.builder()
                    .recipient(recipient)
                    .actor(actor)
                    .type(NotificationType.COMMENT)
                    .post(post)
                    .comment(comment)
                    .build();
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.warn("Failed to emit COMMENT notification for actor {} to recipient {}: {}", actor.getId(), recipient.getId(), e.getMessage());
        }
    }

    /**
     * Emit a COMMENT_LIKE notification when a user likes someone else's comment.
     * Deduplicates within 24 hours (keyed by comment).
     */
    @Transactional
    public void emitCommentLike(User actor, User recipient, Post post, Comment comment) {
        if (actor.getId().equals(recipient.getId())) {
            return;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        if (notificationRepository.existsRecentNotificationForComment(recipient, actor, comment, NotificationType.COMMENT_LIKE, cutoff)) {
            return;
        }

        try {
            Notification notification = Notification.builder()
                    .recipient(recipient)
                    .actor(actor)
                    .type(NotificationType.COMMENT_LIKE)
                    .post(post)
                    .comment(comment)
                    .build();
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.warn("Failed to emit COMMENT_LIKE notification for actor {} to recipient {}: {}", actor.getId(), recipient.getId(), e.getMessage());
        }
    }

    /**
     * Emit a REPLY notification when a user replies to someone else's comment.
     * Always creates; no deduplication.
     */
    @Transactional
    public void emitReply(User actor, User recipient, Post post, Comment replyComment) {
        if (actor.getId().equals(recipient.getId())) {
            return;
        }

        try {
            Notification notification = Notification.builder()
                    .recipient(recipient)
                    .actor(actor)
                    .type(NotificationType.REPLY)
                    .post(post)
                    .comment(replyComment)
                    .build();
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.warn("Failed to emit REPLY notification for actor {} to recipient {}: {}", actor.getId(), recipient.getId(), e.getMessage());
        }
    }

    /**
     * Emit a FOLLOW notification when a user follows someone else.
     * Always creates; no deduplication.
     */
    @Transactional
    public void emitFollow(User actor, User recipient) {
        if (actor.getId().equals(recipient.getId())) {
            return; // Self-follow, skip
        }

        try {
            Notification notification = Notification.builder()
                    .recipient(recipient)
                    .actor(actor)
                    .type(NotificationType.FOLLOW)
                    .build();
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.warn("Failed to emit FOLLOW notification for actor {} to recipient {}: {}", actor.getId(), recipient.getId(), e.getMessage());
        }
    }

    /**
     * Get paginated notifications for a user, newest first.
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> findForUser(User currentUser, Pageable pageable) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(currentUser.getId(), pageable)
                .map(NotificationResponse::from);
    }

    /**
     * Get unread notification count for a user.
     */
    @Transactional(readOnly = true)
    public long unreadCount(User currentUser) {
        return notificationRepository.countByRecipientIdAndReadAtIsNull(currentUser.getId());
    }

    /**
     * Mark all unread notifications as read for a user.
     */
    @Transactional
    public long markAllAsRead(User currentUser) {
        long count = unreadCount(currentUser);
        notificationRepository.markAllAsReadForRecipient(currentUser.getId());
        return count;
    }

    /**
     * Mark a single notification as read. Throws 404 if not found, 403 if not the recipient.
     */
    @Transactional
    public void markAsRead(Long notificationId, User currentUser) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found"));

        if (!notification.getRecipient().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Cannot mark notification of another user as read");
        }

        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }
}
