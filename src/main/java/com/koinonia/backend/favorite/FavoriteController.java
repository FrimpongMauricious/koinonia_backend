package com.koinonia.backend.favorite;

import com.koinonia.backend.post.dto.PostResponse;
import com.koinonia.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/api/v1/posts/{postId}/favorite")
    public PostResponse favorite(@PathVariable Long postId, Authentication authentication) {
        return favoriteService.favorite((User) authentication.getPrincipal(), postId);
    }

    @DeleteMapping("/api/v1/posts/{postId}/favorite")
    public PostResponse unfavorite(@PathVariable Long postId, Authentication authentication) {
        return favoriteService.unfavorite((User) authentication.getPrincipal(), postId);
    }

    @GetMapping("/api/v1/users/me/favorites")
    public Page<PostResponse> myFavorites(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        return favoriteService.listMyFavorites((User) authentication.getPrincipal(), pageable);
    }
}
