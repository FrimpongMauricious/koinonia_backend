package com.koinonia.backend.repost;

import com.koinonia.backend.post.dto.PostResponse;
import com.koinonia.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class RepostController {

    private final RepostService repostService;

    @PostMapping("/{postId}/repost")
    public PostResponse repost(@PathVariable Long postId, Authentication authentication) {
        return repostService.repost((User) authentication.getPrincipal(), postId);
    }

    @DeleteMapping("/{postId}/repost")
    public PostResponse unRepost(@PathVariable Long postId, Authentication authentication) {
        return repostService.unRepost((User) authentication.getPrincipal(), postId);
    }
}
