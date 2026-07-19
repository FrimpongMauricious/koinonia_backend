package com.koinonia.backend.post.dto;

import com.koinonia.backend.post.Post;
import com.koinonia.backend.post.Topic;
import com.koinonia.backend.user.User;
import com.koinonia.backend.user.VerificationTier;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PostResponse {

    private Long id;
    private String title;
    private Topic topic;
    private String content;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private AuthorDto author;
    private long likeCount;
    private long commentCount;
    private boolean likedByCurrentUser;
    private long repostCount;
    private boolean repostedByCurrentUser;
    private boolean favoritedByCurrentUser;
    private long favoriteCount;
    private long viewCount;

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

    public static PostResponse from(Post post,
                                    long likeCount,
                                    long commentCount,
                                    boolean likedByCurrentUser,
                                    long repostCount,
                                    boolean repostedByCurrentUser,
                                    boolean favoritedByCurrentUser,
                                    long favoriteCount,
                                    boolean authorFollowedByCurrentUser,
                                    long viewCount) {
        User u = post.getUser();
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .topic(post.getTopic())
                .content(post.getContent())
                .imageUrl(post.getImageUrl())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .author(AuthorDto.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .displayName(u.getDisplayName())
                        .profilePictureUrl(u.getProfilePictureUrl())
                        .followedByCurrentUser(authorFollowedByCurrentUser)
                        .verificationTier(u.getVerificationTier())
                        .build())
                .likeCount(likeCount)
                .commentCount(commentCount)
                .likedByCurrentUser(likedByCurrentUser)
                .repostCount(repostCount)
                .repostedByCurrentUser(repostedByCurrentUser)
                .favoritedByCurrentUser(favoritedByCurrentUser)
                .favoriteCount(favoriteCount)
                .viewCount(viewCount)
                .build();
    }
}
