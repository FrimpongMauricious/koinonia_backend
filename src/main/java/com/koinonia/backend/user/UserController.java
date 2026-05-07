package com.koinonia.backend.user;

import com.koinonia.backend.follow.FollowRepository;
import com.koinonia.backend.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final FollowRepository followRepository;

    @GetMapping("/me")
    public UserProfileResponse me(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        long followerCount  = followRepository.countByFollowingId(user.getId());
        long followingCount = followRepository.countByFollowerId(user.getId());
        // followedByCurrentUser is always false for /me — you cannot follow yourself
        return UserProfileResponse.from(user, followerCount, followingCount, false);
    }
}
