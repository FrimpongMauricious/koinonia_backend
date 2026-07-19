package com.koinonia.backend.user.dto;

import com.koinonia.backend.user.User;
import com.koinonia.backend.user.VerificationTier;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicUserResponse {
    private Long id;
    private String username;
    private String displayName;
    private String bio;
    private String profilePictureUrl;
    private VerificationTier verificationTier;

    public static PublicUserResponse from(User user) {
        return PublicUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .bio(user.getBio())
                .profilePictureUrl(user.getProfilePictureUrl())
                .verificationTier(user.getVerificationTier())
                .build();
    }
}
