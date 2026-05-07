package com.koinonia.backend.follow.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FollowResponse {
    private Long userId;
    private long followerCount;
    private long followingCount;
    private boolean followedByCurrentUser;
}
