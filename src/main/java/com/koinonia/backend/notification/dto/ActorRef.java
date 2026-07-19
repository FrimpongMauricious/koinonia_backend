package com.koinonia.backend.notification.dto;

import com.koinonia.backend.user.User;
import com.koinonia.backend.user.VerificationTier;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ActorRef {
    private Long id;
    private String username;
    private String displayName;
    private String profilePictureUrl;
    private VerificationTier verificationTier;

    public static ActorRef from(User user) {
        return new ActorRef(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getProfilePictureUrl(),
                user.getVerificationTier()
        );
    }
}
