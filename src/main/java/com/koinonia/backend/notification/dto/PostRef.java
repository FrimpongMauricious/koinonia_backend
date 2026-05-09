package com.koinonia.backend.notification.dto;

import com.koinonia.backend.post.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostRef {
    private Long id;
    private String contentPreview;

    public static PostRef from(Post post) {
        if (post == null) {
            return null;
        }
        String preview = post.getContent();
        if (preview != null && preview.length() > 80) {
            preview = preview.substring(0, 80);
        }
        return new PostRef(post.getId(), preview);
    }
}
