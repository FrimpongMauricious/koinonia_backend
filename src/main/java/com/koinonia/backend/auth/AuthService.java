// Handles registration and login. Intentionally does NOT extend UserDetailsService —
// Spring Security's standard auth chain is bypassed in favour of direct JWT issuance.
package com.koinonia.backend.auth;

import com.koinonia.backend.auth.dto.AuthResponse;
import com.koinonia.backend.auth.dto.LoginRequest;
import com.koinonia.backend.auth.dto.RegisterRequest;
import com.koinonia.backend.auth.jwt.JwtService;
import com.koinonia.backend.exception.ValidationException;
import com.koinonia.backend.user.User;
import com.koinonia.backend.user.UserRepository;
import com.koinonia.backend.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (userRepository.existsByUsername(request.getUsername())) {
            errors.put("username", "This username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            errors.put("email", "An account with this email already exists");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException("Validation failed", errors);
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName())
                .build();

        // Fallback for a race between the existence checks above and this insert;
        // GlobalExceptionHandler maps the resulting DataIntegrityViolationException to 409 Conflict.
        User saved = userRepository.save(user);

        return new AuthResponse(jwtService.generateToken(saved), UserResponse.from(saved));
    }

    public AuthResponse login(LoginRequest request) {
        String identifier = request.getEmailOrUsername();
        User user = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return new AuthResponse(jwtService.generateToken(user), UserResponse.from(user));
    }
}
