package com.koinonia.backend.like;

import com.koinonia.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts/{postId}/like")
@RequiredArgsConstructor
public class PostLikeController {

    private final PostLikeService postLikeService;

    @PostMapping
    public LikeResponse like(@PathVariable Long postId, Authentication authentication) {
        return postLikeService.likePost(postId, (User) authentication.getPrincipal());
    }

    @DeleteMapping
    public LikeResponse unlike(@PathVariable Long postId, Authentication authentication) {
        return postLikeService.unlikePost(postId, (User) authentication.getPrincipal());
    }
}
