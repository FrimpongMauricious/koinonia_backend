package com.koinonia.backend.comment.dto;

import com.koinonia.backend.comment.Comment;
import com.koinonia.backend.user.User;
import com.koinonia.backend.user.VerificationTier;
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
    private Long parentId;
    private long replyCount;
    private long likeCount;
    private boolean likedByCurrentUser;

    @Getter
    @Builder
    public static class AuthorDto {
        private Long id;
        private String username;
        private String displayName;
        private String profilePictureUrl;
        private boolean followedByCurrentUser;
        private VerificationTier verificationTier;
    }

    public static CommentResponse from(Comment comment) {
        return from(comment, false, 0L, false, 0L);
    }

    public static CommentResponse from(Comment comment, boolean authorFollowedByCurrentUser) {
        return from(comment, authorFollowedByCurrentUser, 0L, false, 0L);
    }

    public static CommentResponse from(Comment comment,
                                       boolean authorFollowedByCurrentUser,
                                       long likeCount,
                                       boolean likedByCurrentUser,
                                       long replyCount) {
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
                        .verificationTier(u.getVerificationTier())
                        .build())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .replyCount(replyCount)
                .likeCount(likeCount)
                .likedByCurrentUser(likedByCurrentUser)
                .build();
    }
}
