package com.koinonia.backend.comment;

import com.koinonia.backend.comment.dto.CommentResponse;
import com.koinonia.backend.comment.dto.CreateCommentRequest;
import com.koinonia.backend.comment.dto.UpdateCommentRequest;
import com.koinonia.backend.exception.BadRequestException;
import com.koinonia.backend.exception.CommentNotFoundException;
import com.koinonia.backend.exception.ForbiddenException;
import com.koinonia.backend.exception.PostNotFoundException;
import com.koinonia.backend.follow.FollowRepository;
import com.koinonia.backend.notification.NotificationService;
import com.koinonia.backend.streak.UserStreakService;
import com.koinonia.backend.post.PostRepository;
import com.koinonia.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final FollowRepository followRepository;
    private final NotificationService notificationService;
    private final UserStreakService userStreakService;

    @Transactional
    public CommentResponse createComment(Long postId, CreateCommentRequest request, User currentUser) {
        var post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        Comment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new CommentNotFoundException(request.getParentId()));
            if (!parent.getPost().getId().equals(postId)) {
                throw new BadRequestException("Parent comment does not belong to this post");
            }
            if (parent.getParent() != null) {
                throw new BadRequestException("Cannot reply to a reply — replies must be on top-level comments");
            }
        }

        Comment comment = Comment.builder()
                .user(currentUser)
                .post(post)
                .parent(parent)
                .content(request.getContent())
                .build();
        Comment saved = commentRepository.save(comment);

        // Always notify the post author
        notificationService.emitComment(currentUser, post.getUser(), post, saved);

        // Also notify the parent comment's author when this is a reply
        if (parent != null) {
            notificationService.emitReply(currentUser, parent.getUser(), post, saved);
        }

        // Streak only for top-level comments
        if (parent == null) {
            userStreakService.recordActivity(currentUser.getId());
        }

        return CommentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(Long postId, Pageable pageable) {
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException(postId);
        }
        Page<Comment> page = commentRepository.findByPostIdAndParentIsNull(postId, pageable);
        return enrichPage(page, getCurrentUser());
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getReplies(Long commentId, Pageable pageable) {
        Comment parent = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
        Page<Comment> page = commentRepository.findByParentId(parent.getId(), pageable);
        return enrichPage(page, getCurrentUser());
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

    // ── private helpers ───────────────────────────────────────────────────────

    private Page<CommentResponse> enrichPage(Page<Comment> page, User currentUser) {
        List<Comment> comments = page.getContent();
        if (comments.isEmpty()) return page.map(c -> CommentResponse.from(c));

        List<Long> commentIds = comments.stream().map(Comment::getId).toList();

        Map<Long, Long> likeCounts = toLongMap(commentLikeRepository.countByCommentIdIn(commentIds));
        Map<Long, Long> replyCounts = toLongMap(commentRepository.countRepliesByParentIds(commentIds));

        Set<Long> likedIds = currentUser != null
                ? Set.copyOf(commentLikeRepository.findLikedCommentIds(currentUser.getId(), commentIds))
                : Set.of();

        Set<Long> authorIds = comments.stream()
                .map(c -> c.getUser().getId())
                .collect(Collectors.toSet());

        Set<Long> followedAuthorIds;
        if (currentUser != null && !authorIds.isEmpty()) {
            Set<Long> others = authorIds.stream()
                    .filter(id -> !id.equals(currentUser.getId()))
                    .collect(Collectors.toSet());
            followedAuthorIds = others.isEmpty()
                    ? Set.of()
                    : followRepository.findFollowedAuthorIds(currentUser.getId(), others);
        } else {
            followedAuthorIds = Set.of();
        }

        return page.map(c -> CommentResponse.from(
                c,
                followedAuthorIds.contains(c.getUser().getId()),
                likeCounts.getOrDefault(c.getId(), 0L),
                likedIds.contains(c.getId()),
                replyCounts.getOrDefault(c.getId(), 0L)
        ));
    }

    private static Map<Long, Long> toLongMap(List<Object[]> rows) {
        return rows.stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
    }

    private User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User u) {
            return u;
        }
        return null;
    }
}
