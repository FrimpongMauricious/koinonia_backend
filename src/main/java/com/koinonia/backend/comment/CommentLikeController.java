package com.koinonia.backend.comment;

import com.koinonia.backend.comment.dto.CommentLikeResponse;
import com.koinonia.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/comments/{id}/like")
@RequiredArgsConstructor
public class CommentLikeController {

    private final CommentLikeService commentLikeService;

    @PostMapping
    public CommentLikeResponse like(@PathVariable Long id, Authentication authentication) {
        return commentLikeService.likeComment(id, (User) authentication.getPrincipal());
    }

    @DeleteMapping
    public CommentLikeResponse unlike(@PathVariable Long id, Authentication authentication) {
        return commentLikeService.unlikeComment(id, (User) authentication.getPrincipal());
    }
}
