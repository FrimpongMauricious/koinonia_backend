package com.koinonia.backend.comment;

import com.koinonia.backend.comment.dto.CommentLikeResponse;
import com.koinonia.backend.exception.CommentNotFoundException;
import com.koinonia.backend.notification.NotificationService;
import com.koinonia.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentLikeService {

    private final CommentLikeRepository commentLikeRepository;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;

    @Transactional
    public CommentLikeResponse likeComment(Long commentId, User currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        if (!commentLikeRepository.existsByUserIdAndCommentId(currentUser.getId(), commentId)) {
            commentLikeRepository.save(CommentLike.builder()
                    .user(currentUser)
                    .comment(comment)
                    .build());
            notificationService.emitCommentLike(currentUser, comment.getUser(), comment.getPost(), comment);
        }
        return new CommentLikeResponse(commentLikeRepository.countByCommentId(commentId), true);
    }

    @Transactional
    public CommentLikeResponse unlikeComment(Long commentId, User currentUser) {
        if (!commentRepository.existsById(commentId)) {
            throw new CommentNotFoundException(commentId);
        }
        commentLikeRepository.deleteByUserIdAndCommentId(currentUser.getId(), commentId);
        return new CommentLikeResponse(commentLikeRepository.countByCommentId(commentId), false);
    }
}
