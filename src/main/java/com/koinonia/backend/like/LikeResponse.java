package com.koinonia.backend.like;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class LikeResponse {
    private final long likeCount;
    private final boolean likedByCurrentUser;
}
