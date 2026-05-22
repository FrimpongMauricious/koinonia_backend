package com.koinonia.backend.user.dto;

import com.koinonia.backend.user.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String displayName;
    private String bio;
    private String profilePictureUrl;
    private LocalDateTime createdAt;
    private long followerCount;
    private long followingCount;
    private boolean followedByCurrentUser;
    private long totalLikes;
    private int currentStreak;
    private int longestStreak;

    public static UserProfileResponse from(User user,
                                           long followerCount,
                                           long followingCount,
                                           boolean followedByCurrentUser,
                                           long totalLikes,
                                           int currentStreak,
                                           int longestStreak) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .bio(user.getBio())
                .profilePictureUrl(user.getProfilePictureUrl())
                .createdAt(user.getCreatedAt())
                .followerCount(followerCount)
                .followingCount(followingCount)
                .followedByCurrentUser(followedByCurrentUser)
                .totalLikes(totalLikes)
                .currentStreak(currentStreak)
                .longestStreak(longestStreak)
                .build();
    }
}
