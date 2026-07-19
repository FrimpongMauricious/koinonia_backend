package com.koinonia.backend.like;

import com.koinonia.backend.exception.PostNotFoundException;
import com.koinonia.backend.notification.NotificationService;
import com.koinonia.backend.post.Post;
import com.koinonia.backend.post.PostRepository;
import com.koinonia.backend.user.User;
import com.koinonia.backend.user.VerificationTierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final NotificationService notificationService;
    private final VerificationTierService verificationTierService;

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
        verificationTierService.recomputeAndSave(post.getUser());
        return new LikeResponse(postLikeRepository.countByPostId(postId), true);
    }

    @Transactional
    public LikeResponse unlikePost(Long postId, User currentUser) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));
        postLikeRepository.findByUserIdAndPostId(currentUser.getId(), postId)
                .ifPresent(postLikeRepository::delete);
        verificationTierService.recomputeAndSave(post.getUser());
        return new LikeResponse(postLikeRepository.countByPostId(postId), false);
    }
}
