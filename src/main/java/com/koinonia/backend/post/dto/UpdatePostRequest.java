package com.koinonia.backend.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePostRequest {

    @NotBlank(message = "Content must not be blank")
    @Size(max = 1000, message = "Content must be 1000 characters or fewer")
    private String content;
}
