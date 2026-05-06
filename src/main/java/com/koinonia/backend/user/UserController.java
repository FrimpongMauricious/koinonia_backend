package com.koinonia.backend.user;

import com.koinonia.backend.user.dto.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    // Authentication is injected by Spring from the SecurityContext populated by JwtAuthenticationFilter.
    // The principal is our User entity because that is what the filter sets.
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
