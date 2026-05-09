package com.koinonia.backend.like;

import com.koinonia.backend.exception.PostNotFoundException;
import com.koinonia.backend.notification.NotificationService;
import com.koinonia.backend.post.Post;
import com.koinonia.backend.post.PostRepository;
import com.koinonia.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final NotificationService notificationService;

    @Transactional
    public LikeResponse likePost(Long postId, User currentUser) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));
        if (!postLikeRepository.existsByUserIdAndPostId(currentUser.getId(), postId)) {
            postLikeRepository.save(PostLike.builder()
                    .user(currentUser)
                    .post(post)
                    .build());
            notificationService.emitLike(currentUser, post.getUser(), post);
        }
        return new LikeResponse(postLikeRepository.countByPostId(postId), true);
    }

    @Transactional
    public LikeResponse unlikePost(Long postId, User currentUser) {
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException(postId);
        }
        postLikeRepository.findByUserIdAndPostId(currentUser.getId(), postId)
                .ifPresent(postLikeRepository::delete);
        return new LikeResponse(postLikeRepository.countByPostId(postId), false);
    }
}
