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
class AuthorFollowEnrichmentTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void authorFollowedByCurrentUserOnPostsAndComments() throws Exception {
        String tokenA = registerAndLogin("enrichA", "enrichA@koinonia.dev", "Password1!");
        String tokenB = registerAndLogin("enrichB", "enrichB@koinonia.dev", "Password1!");
        long userAId = getUserId(tokenA);
        long userBId = getUserId(tokenB);

        long postId = createPost(tokenB, "Hello from B");

        mockMvc.perform(post("/api/v1/users/" + userBId + "/follow")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        createComment(tokenA, postId, "Nice post!");

        // ── Posts ─────────────────────────────────────────────────────────────

        // anonymous → false
        mockMvc.perform(get("/api/v1/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].author.followedByCurrentUser").value(false));

        // as A (follows B) → true
        mockMvc.perform(get("/api/v1/posts")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].author.followedByCurrentUser").value(true));

        // as B (self-author) → false
        mockMvc.perform(get("/api/v1/posts")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].author.followedByCurrentUser").value(false));

        // ── Comments ──────────────────────────────────────────────────────────

        String commentsUrl = "/api/v1/posts/" + postId + "/comments";

        // anonymous → false
        mockMvc.perform(get(commentsUrl))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].author.followedByCurrentUser").value(false));

        // as B (doesn't follow A yet) → false
        mockMvc.perform(get(commentsUrl)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].author.followedByCurrentUser").value(false));

        // as A (self-authored comment) → false
        mockMvc.perform(get(commentsUrl)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].author.followedByCurrentUser").value(false));

        // B follows A → B now sees A's comment as followedByCurrentUser=true
        mockMvc.perform(post("/api/v1/users/" + userAId + "/follow")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());

        mockMvc.perform(get(commentsUrl)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].author.followedByCurrentUser").value(true));
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

    private void createComment(String token, long postId, String content) throws Exception {
        mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", content))))
                .andExpect(status().isCreated());
    }
}
