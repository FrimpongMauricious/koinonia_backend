package com.koinonia.backend.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Pattern(
        regexp = "^[a-zA-Z0-9_]{3,50}$",
        message = "Username must be 3–50 characters and contain only letters, numbers, or underscores"
    )
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
        message = "Password must be at least 8 characters and contain at least one letter and one number"
    )
    private String password;

    private String displayName; // optional
}
