package com.koinonia.backend.user.dto;

import com.koinonia.backend.user.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PublicUserProfileResponse {
    private Long id;
    private String username;
    private String displayName;
    private String bio;
    private String profilePictureUrl;
    private LocalDateTime createdAt;
    private long followerCount;
    private long followingCount;
    private boolean followedByCurrentUser;
    private long totalLikes;

    public static PublicUserProfileResponse from(User user,
                                                 long followerCount,
                                                 long followingCount,
                                                 boolean followedByCurrentUser,
                                                 long totalLikes) {
        return PublicUserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .bio(user.getBio())
                .profilePictureUrl(user.getProfilePictureUrl())
                .createdAt(user.getCreatedAt())
                .followerCount(followerCount)
                .followingCount(followingCount)
                .followedByCurrentUser(followedByCurrentUser)
                .totalLikes(totalLikes)
                .build();
    }
}
