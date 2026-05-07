package com.koinonia.backend.favorite;

import com.koinonia.backend.exception.PostNotFoundException;
import com.koinonia.backend.post.Post;
import com.koinonia.backend.post.PostRepository;
import com.koinonia.backend.post.PostService;
import com.koinonia.backend.post.dto.PostResponse;
import com.koinonia.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final PostRepository postRepository;
    private final PostService postService;

    @Transactional
    public PostResponse favorite(User currentUser, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        if (!favoriteRepository.existsByUserIdAndPostId(currentUser.getId(), postId)) {
            favoriteRepository.save(Favorite.builder()
                    .user(currentUser)
                    .post(post)
                    .build());
        }

        return postService.enrichPosts(List.of(post)).get(0);
    }

    @Transactional
    public PostResponse unfavorite(User currentUser, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        favoriteRepository.findByUserIdAndPostId(currentUser.getId(), postId)
                .ifPresent(favoriteRepository::delete);

        return postService.enrichPosts(List.of(post)).get(0);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> listMyFavorites(User currentUser, Pageable pageable) {
        Page<Favorite> page = favoriteRepository.findByUserId(currentUser.getId(), pageable);
        List<Post> posts = page.getContent().stream().map(Favorite::getPost).toList();
        List<PostResponse> responses = postService.enrichPosts(posts);
        return new PageImpl<>(responses, pageable, page.getTotalElements());
    }
}
