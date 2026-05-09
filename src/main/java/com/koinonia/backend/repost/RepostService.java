package com.koinonia.backend.repost;

import com.koinonia.backend.exception.BadRequestException;
import com.koinonia.backend.exception.PostNotFoundException;
import com.koinonia.backend.notification.NotificationService;
import com.koinonia.backend.post.Post;
import com.koinonia.backend.post.PostRepository;
import com.koinonia.backend.post.PostService;
import com.koinonia.backend.post.dto.PostResponse;
import com.koinonia.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RepostService {

    private final RepostRepository repostRepository;
    private final PostRepository postRepository;
    private final PostService postService;
    private final NotificationService notificationService;

    @Transactional
    public PostResponse repost(User currentUser, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        if (post.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Cannot repost your own post");
        }

        if (!repostRepository.existsByUserIdAndPostId(currentUser.getId(), postId)) {
            repostRepository.save(Repost.builder()
                    .user(currentUser)
                    .post(post)
                    .build());
            notificationService.emitRepost(currentUser, post.getUser(), post);
        }

        return postService.enrichPosts(List.of(post)).get(0);
    }

    @Transactional
    public PostResponse unRepost(User currentUser, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        repostRepository.findByUserIdAndPostId(currentUser.getId(), postId)
                .ifPresent(repostRepository::delete);

        return postService.enrichPosts(List.of(post)).get(0);
    }
}
