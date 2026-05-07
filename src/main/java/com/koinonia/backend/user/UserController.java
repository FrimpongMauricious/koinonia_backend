package com.koinonia.backend.user;

import com.koinonia.backend.exception.UserNotFoundException;
import com.koinonia.backend.follow.FollowRepository;
import com.koinonia.backend.user.dto.DeleteAccountRequest;
import com.koinonia.backend.user.dto.PublicUserProfileResponse;
import com.koinonia.backend.user.dto.UpdateProfileRequest;
import com.koinonia.backend.user.dto.UserProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    @GetMapping("/me")
    public UserProfileResponse me(Authentication authentication) {
        return userService.getProfile((User) authentication.getPrincipal());
    }

    @PutMapping("/me")
    public UserProfileResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request,
                                             Authentication authentication) {
        return userService.updateProfile((User) authentication.getPrincipal(), request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@Valid @RequestBody DeleteAccountRequest request,
                              Authentication authentication) {
        userService.deleteAccount((User) authentication.getPrincipal(), request);
    }

    @GetMapping("/{userId}")
    public PublicUserProfileResponse getPublicProfile(
            @PathVariable Long userId,
            @AuthenticationPrincipal(errorOnInvalidType = false) User currentUser) {

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        long followerCount  = followRepository.countByFollowingId(userId);
        long followingCount = followRepository.countByFollowerId(userId);

        boolean followedByCurrentUser = currentUser != null
                && !currentUser.getId().equals(userId)
                && followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), userId);

        return PublicUserProfileResponse.from(target, followerCount, followingCount, followedByCurrentUser);
    }
}
