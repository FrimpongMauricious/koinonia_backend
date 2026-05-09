package com.koinonia.backend.view;

import com.koinonia.backend.post.Post;
import com.koinonia.backend.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostViewService {

    private final PostViewRepository postViewRepository;

    @Transactional
    public void recordView(User viewer, Post post) {
        if (viewer == null) return;
        try {
            Long viewerId = viewer.getId();
            Long postId = post.getId();
            Long authorId = post.getUser().getId();
            if (viewerId.equals(authorId)) return;
            if (postViewRepository.existsByUserIdAndPostId(viewerId, postId)) return;
            postViewRepository.insertById(viewerId, postId);
        } catch (Exception e) {
            log.warn("Failed to record view for post {} viewer {}: {}", post.getId(), viewer.getId(), e.getMessage());
        }
    }
}
