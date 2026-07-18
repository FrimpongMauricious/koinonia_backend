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

    @NotBlank(message = "Content must not be blank")
    @Size(max = 1000, message = "Content must be 1000 characters or fewer")
    private String content;
}
