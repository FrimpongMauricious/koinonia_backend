package com.koinonia.backend.comment.dto;

public record CommentLikeResponse(long likeCount, boolean likedByCurrentUser) {}
