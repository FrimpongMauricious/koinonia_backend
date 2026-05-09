package com.koinonia.backend.user;

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
        return userService.getPublicProfile(userId, currentUser);
    }
}
