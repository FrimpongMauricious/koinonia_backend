package com.koinonia.backend.post.dto;

import com.koinonia.backend.post.Topic;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TopicCountResponse {
    private Topic topic;
    private String displayName;
    private long postCount;
}
