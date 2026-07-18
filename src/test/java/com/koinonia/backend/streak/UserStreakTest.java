package com.koinonia.backend.streak;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koinonia.backend.auth.dto.LoginRequest;
import com.koinonia.backend.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserStreakTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserStreakRepository userStreakRepository;

    @Test
    void streakLifecycle() throws Exception {
        String token = registerAndLogin("streakUser", "streakUser@koinonia.dev", "Password1!");
        long userId = getUserId(token);

        // Step 2: first post → streak = 1, longestStreak = 1, lastActivityDate = today
        createPost(token, "First post");
        mockMvc.perform(get("/api/v1/users/me/streak")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(1))
                .andExpect(jsonPath("$.longestStreak").value(1))
                .andExpect(jsonPath("$.lastActivityDate").isNotEmpty());

        // Step 3: second post same day → streak stays at 1 (no double-count)
        createPost(token, "Second post same day");
        mockMvc.perform(get("/api/v1/users/me/streak")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(1));

        // Step 4: comment same day → streak stays at 1
        long postId = getFirstPostId(token);
        createComment(token, postId, "A comment");
        mockMvc.perform(get("/api/v1/users/me/streak")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(1));

        // Step 5: simulate next day — set lastActivityDate to yesterday, then post
        UserStreak streak = userStreakRepository.findById(userId).orElseThrow();
        streak.setLastActivityDate(LocalDate.now(ZoneOffset.UTC).minusDays(1));
        userStreakRepository.saveAndFlush(streak);

        createPost(token, "Post on day 2");
        mockMvc.perform(get("/api/v1/users/me/streak")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(2))
                .andExpect(jsonPath("$.longestStreak").value(2));

        // Step 6: simulate streak break — set lastActivityDate to 3 days ago, then post
        streak = userStreakRepository.findById(userId).orElseThrow();
        streak.setLastActivityDate(LocalDate.now(ZoneOffset.UTC).minusDays(3));
        userStreakRepository.saveAndFlush(streak);

        createPost(token, "Post after break");
        mockMvc.perform(get("/api/v1/users/me/streak")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(1))
                .andExpect(jsonPath("$.longestStreak").value(2));

        // Step 7: public endpoint returns streak data
        mockMvc.perform(get("/api/v1/users/" + userId + "/streak"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").isNumber())
                .andExpect(jsonPath("$.longestStreak").isNumber());

        // Step 8: non-existent user → 404
        mockMvc.perform(get("/api/v1/users/99999/streak"))
                .andExpect(status().isNotFound());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String registerAndLogin(String username, String email, String password) throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername(username);
        reg.setEmail(email);
        reg.setPassword(password);
        reg.setDisplayName(username);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest();
        login.setEmailOrUsername(email);
        login.setPassword(password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private long getUserId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createPost(String token, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", content, "topic", "FAITH"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long getFirstPostId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/posts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("content").get(0).get("id").asLong();
    }

    private void createComment(String token, long postId, String content) throws Exception {
        mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", content))))
                .andExpect(status().isCreated());
    }
}
