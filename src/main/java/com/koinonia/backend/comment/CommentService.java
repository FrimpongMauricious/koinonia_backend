package com.koinonia.backend.comment;

import com.koinonia.backend.comment.dto.CommentResponse;
import com.koinonia.backend.comment.dto.CreateCommentRequest;
import com.koinonia.backend.comment.dto.UpdateCommentRequest;
import com.koinonia.backend.exception.CommentNotFoundException;
import com.koinonia.backend.exception.ForbiddenException;
import com.koinonia.backend.exception.PostNotFoundException;
import com.koinonia.backend.follow.FollowRepository;
import com.koinonia.backend.post.PostRepository;
import com.koinonia.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final FollowRepository followRepository;

    @Transactional
    public CommentResponse createComment(Long postId, CreateCommentRequest request, User currentUser) {
        var post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));
        Comment comment = Comment.builder()
                .user(currentUser)
                .post(post)
                .content(request.getContent())
                .build();
        return CommentResponse.from(commentRepository.save(comment));
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(Long postId, Pageable pageable) {
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException(postId);
        }
        Page<Comment> page = commentRepository.findByPostId(postId, pageable);

        User currentUser = getCurrentUser();

        Set<Long> authorIds = page.getContent().stream()
                .map(c -> c.getUser().getId())
                .collect(Collectors.toSet());

        Set<Long> followedAuthorIds;
        if (currentUser != null && !authorIds.isEmpty()) {
            Set<Long> otherAuthorIds = authorIds.stream()
                    .filter(id -> !id.equals(currentUser.getId()))
                    .collect(Collectors.toSet());
            followedAuthorIds = otherAuthorIds.isEmpty()
                    ? Set.of()
                    : followRepository.findFollowedAuthorIds(currentUser.getId(), otherAuthorIds);
        } else {
            followedAuthorIds = Set.of();
        }

        return page.map(c -> CommentResponse.from(c, followedAuthorIds.contains(c.getUser().getId())));
    }

    private User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User u) {
            return u;
        }
        return null;
    }

    @Transactional
    public CommentResponse updateComment(Long id, UpdateCommentRequest request, User currentUser) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new CommentNotFoundException(id));
        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You are not the author of this comment");
        }
        comment.setContent(request.getContent());
        return CommentResponse.from(commentRepository.saveAndFlush(comment));
    }

    @Transactional
    public void deleteComment(Long id, User currentUser) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new CommentNotFoundException(id));
        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You are not the author of this comment");
        }
        commentRepository.delete(comment);
    }
}
