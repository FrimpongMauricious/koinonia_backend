package com.koinonia.backend.streak;

import com.koinonia.backend.streak.dto.UserStreakResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserStreakService {

    private final UserStreakRepository userStreakRepository;

    @Transactional
    public void recordActivity(Long userId) {
        try {
            UserStreak streak = userStreakRepository.findById(userId).orElseGet(() -> {
                UserStreak s = new UserStreak();
                s.setUserId(userId);
                s.setCurrentStreak(0);
                s.setLongestStreak(0);
                s.setLastActivityDate(null);
                s.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
                return s;
            });

            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            LocalDate last  = streak.getLastActivityDate();

            if (today.equals(last)) {
                return;
            }

            if (last != null && today.equals(last.plusDays(1))) {
                streak.setCurrentStreak(streak.getCurrentStreak() + 1);
            } else {
                streak.setCurrentStreak(1);
            }

            if (streak.getCurrentStreak() > streak.getLongestStreak()) {
                streak.setLongestStreak(streak.getCurrentStreak());
            }

            streak.setLastActivityDate(today);
            streak.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            userStreakRepository.save(streak);
        } catch (Exception e) {
            log.warn("Failed to record streak for user {}: {}", userId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public UserStreakResponse getStreak(Long userId) {
        return userStreakRepository.findById(userId)
                .map(s -> new UserStreakResponse(s.getCurrentStreak(), s.getLongestStreak(), s.getLastActivityDate()))
                .orElse(new UserStreakResponse(0, 0, null));
    }
}
