package com.koinonia.backend.post.dto;

import com.koinonia.backend.post.Post;
import com.koinonia.backend.user.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PostResponse {

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
    }

    public static PostResponse from(Post post) {
        User u = post.getUser();
        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .author(AuthorDto.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .displayName(u.getDisplayName())
                        .profilePictureUrl(u.getProfilePictureUrl())
                        .build())
                .build();
    }
}
