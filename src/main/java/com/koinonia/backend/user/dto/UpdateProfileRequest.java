package com.koinonia.backend.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

@Getter
@Setter
public class UpdateProfileRequest {

    @Size(min = 2, max = 50, message = "Display name must be 2-50 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9 .'-]+$",
        message = "Display name can only contain letters, numbers, spaces, dots, hyphens, and apostrophes"
    )
    private String displayName;

    @Size(max = 200, message = "Bio must be 200 characters or fewer")
    private String bio;

    @Size(max = 500, message = "Profile picture URL must be 500 characters or fewer")
    @URL(message = "Profile picture URL must be a valid URL")
    private String profilePictureUrl;
}
