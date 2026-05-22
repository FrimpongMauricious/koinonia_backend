package com.koinonia.backend.streak.dto;

import java.time.LocalDate;

public record UserStreakResponse(int currentStreak, int longestStreak, LocalDate lastActivityDate) {}
