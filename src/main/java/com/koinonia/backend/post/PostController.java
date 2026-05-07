package com.koinonia.backend.post;

import com.koinonia.backend.post.dto.CreatePostRequest;
import com.koinonia.backend.post.dto.PostResponse;
import com.koinonia.backend.post.dto.UpdatePostRequest;
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
public class PostController {

    private final PostService postService;

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse createPost(@Valid @RequestBody CreatePostRequest request,
                                   Authentication authentication) {
        return postService.createPost(request, authentication);
    }

    @GetMapping("/posts")
    public Page<PostResponse> getFeed(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return postService.getFeed(pageable);
    }

    @GetMapping("/posts/{id}")
    public PostResponse getPost(@PathVariable Long id) {
        return postService.getPostById(id);
    }

    @GetMapping("/users/{userId}/posts")
    public Page<PostResponse> getUserPosts(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return postService.getPostsByUser(userId, pageable);
    }

    @PutMapping("/posts/{id}")
    public PostResponse updatePost(@PathVariable Long id,
                                   @Valid @RequestBody UpdatePostRequest request,
                                   Authentication authentication) {
        return postService.updatePost(id, request, authentication);
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@PathVariable Long id, Authentication authentication) {
        postService.deletePost(id, authentication);
    }
}
