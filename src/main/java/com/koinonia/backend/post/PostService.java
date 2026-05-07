package com.koinonia.backend.post;

import com.koinonia.backend.exception.ForbiddenException;
import com.koinonia.backend.exception.PostNotFoundException;
import com.koinonia.backend.post.dto.CreatePostRequest;
import com.koinonia.backend.post.dto.PostResponse;
import com.koinonia.backend.post.dto.UpdatePostRequest;
import com.koinonia.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    @Transactional
    public PostResponse createPost(CreatePostRequest request, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        Post post = Post.builder()
                .user(currentUser)
                .content(request.getContent())
                .build();
        return PostResponse.from(postRepository.save(post));
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getFeed(Pageable pageable) {
        return postRepository.findAll(pageable).map(PostResponse::from);
    }

    @Transactional(readOnly = true)
    public PostResponse getPostById(Long id) {
        return postRepository.findById(id)
                .map(PostResponse::from)
                .orElseThrow(() -> new PostNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getPostsByUser(Long userId, Pageable pageable) {
        return postRepository.findByUserId(userId, pageable).map(PostResponse::from);
    }

    @Transactional
    public PostResponse updatePost(Long id, UpdatePostRequest request, Authentication authentication) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException(id));
        User currentUser = (User) authentication.getPrincipal();
        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You are not the author of this post");
        }
        post.setContent(request.getContent());
        // saveAndFlush triggers @PreUpdate so updatedAt is fresh in the response
        return PostResponse.from(postRepository.saveAndFlush(post));
    }

    @Transactional
    public void deletePost(Long id, Authentication authentication) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException(id));
        User currentUser = (User) authentication.getPrincipal();
        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You are not the author of this post");
        }
        postRepository.delete(post);
    }
}
