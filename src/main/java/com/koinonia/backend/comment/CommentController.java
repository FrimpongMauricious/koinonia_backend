package com.koinonia.backend.comment;

import com.koinonia.backend.comment.dto.CommentResponse;
import com.koinonia.backend.comment.dto.CreateCommentRequest;
import com.koinonia.backend.comment.dto.UpdateCommentRequest;
import com.koinonia.backend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(@PathVariable Long postId,
                                         @Valid @RequestBody CreateCommentRequest request,
                                         Authentication authentication) {
        return commentService.createComment(postId, request, (User) authentication.getPrincipal());
    }

    @GetMapping("/posts/{postId}/comments")
    public Page<CommentResponse> getComments(
            @PathVariable Long postId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return commentService.getComments(postId, pageable);
    }

    @GetMapping("/comments/{id}/replies")
    public Page<CommentResponse> getReplies(
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return commentService.getReplies(id, pageable);
    }

    @PutMapping("/comments/{id}")
    public CommentResponse updateComment(@PathVariable Long id,
                                         @Valid @RequestBody UpdateCommentRequest request,
                                         Authentication authentication) {
        return commentService.updateComment(id, request, (User) authentication.getPrincipal());
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long id, Authentication authentication) {
        commentService.deleteComment(id, (User) authentication.getPrincipal());
    }
}
