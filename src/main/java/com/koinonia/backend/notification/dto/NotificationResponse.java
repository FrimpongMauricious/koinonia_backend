package com.koinonia.backend.notification.dto;

import com.koinonia.backend.notification.Notification;
import com.koinonia.backend.notification.NotificationType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private ActorRef actor;
    private PostRef post;
    private String commentPreview;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime readAt;

    public static NotificationResponse from(Notification notification) {
        String commentPreview = null;
        if (notification.getComment() != null && notification.getComment().getContent() != null) {
            commentPreview = notification.getComment().getContent();
            if (commentPreview.length() > 80) {
                commentPreview = commentPreview.substring(0, 80);
            }
        }

        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .actor(ActorRef.from(notification.getActor()))
                .post(PostRef.from(notification.getPost()))
                .commentPreview(commentPreview)
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
