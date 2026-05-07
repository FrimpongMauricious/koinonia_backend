package com.koinonia.backend.follow;

import com.koinonia.backend.follow.dto.FollowResponse;
import com.koinonia.backend.user.User;
import com.koinonia.backend.user.dto.PublicUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{userId}/follow")
    public FollowResponse follow(@PathVariable Long userId, Authentication authentication) {
        return followService.follow((User) authentication.getPrincipal(), userId);
    }

    @DeleteMapping("/{userId}/follow")
    public FollowResponse unfollow(@PathVariable Long userId, Authentication authentication) {
        return followService.unfollow((User) authentication.getPrincipal(), userId);
    }

    @GetMapping("/{userId}/followers")
    public Page<PublicUserResponse> getFollowers(
            @PathVariable Long userId,
            @PageableDefault(size = 50) Pageable pageable) {
        return followService.getFollowers(userId, pageable);
    }

    @GetMapping("/{userId}/following")
    public Page<PublicUserResponse> getFollowing(
            @PathVariable Long userId,
            @PageableDefault(size = 50) Pageable pageable) {
        return followService.getFollowing(userId, pageable);
    }
}
