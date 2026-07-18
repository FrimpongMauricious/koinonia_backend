package com.koinonia.backend.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be 3-20 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9_]+$",
        message = "Username can only contain letters, numbers, and underscores. No spaces or special characters."
    )
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 50, message = "Password must be 8-50 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#+\\-_])[A-Za-z\\d@$!%*?&#+\\-_]+$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character (@$!%*?&#+_-)"
    )
    private String password;

    @NotBlank(message = "Display name is required")
    @Size(min = 2, max = 50, message = "Display name must be 2-50 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9 .'-]+$",
        message = "Display name can only contain letters, numbers, spaces, dots, hyphens, and apostrophes"
    )
    private String displayName;
}
