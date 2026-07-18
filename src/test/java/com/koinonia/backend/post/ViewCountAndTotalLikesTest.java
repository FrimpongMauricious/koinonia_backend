package com.koinonia.backend.post;

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

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ViewCountAndTotalLikesTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void viewCountAndTotalLikes() throws Exception {
        String tokenA = registerAndLogin("viewA", "viewA@koinonia.dev", "Password1!");
        String tokenB = registerAndLogin("viewB", "viewB@koinonia.dev", "Password1!");
        String tokenC = registerAndLogin("viewC", "viewC@koinonia.dev", "Password1!");
        String tokenD = registerAndLogin("viewD", "viewD@koinonia.dev", "Password1!");
        long userAId = getUserId(tokenA);
        long userBId = getUserId(tokenB);

        long p1 = createPost(tokenA, "Post 1 by A");
        long p2 = createPost(tokenA, "Post 2 by A");
        createPost(tokenB, "Post 3 by B");

        likePost(tokenB, p1);
        likePost(tokenB, p2);
        likePost(tokenC, p1);

        // ── Total likes ────────────────────────────────────────────────────────

        // /me as A → totalLikes = 3 (P1 has 2, P2 has 1)
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLikes").value(3));

        // /users/{A} as B → totalLikes = 3
        mockMvc.perform(get("/api/v1/users/" + userAId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLikes").value(3));

        // /users/{B} anonymous → totalLikes = 0
        mockMvc.perform(get("/api/v1/users/" + userBId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLikes").value(0));

        // ── View counts ────────────────────────────────────────────────────────

        // B opens P1 → viewCount = 1
        mockMvc.perform(get("/api/v1/posts/" + p1)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewCount").value(1));

        // B opens P1 again → still 1 (dedup)
        mockMvc.perform(get("/api/v1/posts/" + p1)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewCount").value(1));

        // C opens P1 → viewCount = 2
        mockMvc.perform(get("/api/v1/posts/" + p1)
                        .header("Authorization", "Bearer " + tokenC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewCount").value(2));

        // A (author) opens P1 → still 2 (self-view skipped)
        mockMvc.perform(get("/api/v1/posts/" + p1)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewCount").value(2));

        // Anonymous opens P1 → still 2, no crash
        mockMvc.perform(get("/api/v1/posts/" + p1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewCount").value(2));

        // Feed (D) → viewCount field present, P1 still at 2 (feed doesn't increment)
        mockMvc.perform(get("/api/v1/posts")
                        .header("Authorization", "Bearer " + tokenD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].viewCount").exists());

        // D now opens P1 via single-post endpoint → viewCount becomes 3
        mockMvc.perform(get("/api/v1/posts/" + p1)
                        .header("Authorization", "Bearer " + tokenD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewCount").value(3));
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

    private void likePost(String token, long postId) throws Exception {
        mockMvc.perform(post("/api/v1/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
