package com.koinonia.backend.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

@Getter
@Setter
public class UpdateProfileRequest {

    @Size(max = 100, message = "Display name must be 100 characters or fewer")
    private String displayName;

    @Size(max = 500, message = "Bio must be 500 characters or fewer")
    private String bio;

    @Size(max = 500, message = "Profile picture URL must be 500 characters or fewer")
    @URL(message = "Profile picture URL must be a valid URL")
    private String profilePictureUrl;
}
