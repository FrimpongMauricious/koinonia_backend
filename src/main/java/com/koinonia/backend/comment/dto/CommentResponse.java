package com.koinonia.backend.comment.dto;

import com.koinonia.backend.comment.Comment;
import com.koinonia.backend.user.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentResponse {

    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private AuthorDto author;

    @Getter
    @Builder
    public static class AuthorDto {
        private Long id;
        private String username;
        private String displayName;
        private String profilePictureUrl;
        private boolean followedByCurrentUser;
    }

    public static CommentResponse from(Comment comment) {
        return from(comment, false);
    }

    public static CommentResponse from(Comment comment, boolean authorFollowedByCurrentUser) {
        User u = comment.getUser();
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .author(AuthorDto.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .displayName(u.getDisplayName())
                        .profilePictureUrl(u.getProfilePictureUrl())
                        .followedByCurrentUser(authorFollowedByCurrentUser)
                        .build())
                .build();
    }
}
