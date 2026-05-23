package com.koinonia.backend.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCommentRequest {

    @NotBlank(message = "Content must not be blank")
    @Size(max = 500, message = "Content must be 500 characters or fewer")
    private String content;

    private Long parentId;
}
