package com.koinonia.backend.post.dto;

import com.koinonia.backend.post.Topic;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePostRequest {

    @Size(max = 100, message = "Title must be 100 characters or fewer")
    private String title;

    // Topic cannot be changed after posting; accepted but ignored by PostService.updatePost.
    private Topic topic;

    @NotBlank(message = "Post content is required")
    @Size(min = 1, max = 1000, message = "Post content must be 1-1000 characters")
    private String content;

    @Size(max = 500, message = "Image URL is too long")
    private String imageUrl;
}
