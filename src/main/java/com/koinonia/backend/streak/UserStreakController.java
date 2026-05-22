package com.koinonia.backend.streak;

import com.koinonia.backend.exception.UserNotFoundException;
import com.koinonia.backend.streak.dto.UserStreakResponse;
import com.koinonia.backend.user.User;
import com.koinonia.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserStreakController {

    private final UserStreakService userStreakService;
    private final UserRepository userRepository;

    @GetMapping("/me/streak")
    public UserStreakResponse getMyStreak(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return userStreakService.getStreak(user.getId());
    }

    @GetMapping("/{userId}/streak")
    public UserStreakResponse getUserStreak(@PathVariable Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        return userStreakService.getStreak(userId);
    }
}
