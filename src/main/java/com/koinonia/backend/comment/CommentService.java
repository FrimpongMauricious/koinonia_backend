package com.koinonia.backend.comment;

import com.koinonia.backend.comment.dto.CommentResponse;
import com.koinonia.backend.comment.dto.CreateCommentRequest;
import com.koinonia.backend.comment.dto.UpdateCommentRequest;
import com.koinonia.backend.exception.CommentNotFoundException;
import com.koinonia.backend.exception.ForbiddenException;
import com.koinonia.backend.exception.PostNotFoundException;
import com.koinonia.backend.post.PostRepository;
import com.koinonia.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

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
        return commentRepository.findByPostId(postId, pageable).map(CommentResponse::from);
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
