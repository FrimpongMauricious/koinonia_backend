package com.koinonia.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koinonia.backend.auth.dto.LoginRequest;
import com.koinonia.backend.auth.dto.RegisterRequest;
import com.koinonia.backend.comment.dto.CreateCommentRequest;
import com.koinonia.backend.post.Topic;
import com.koinonia.backend.post.dto.CreatePostRequest;
import com.koinonia.backend.user.dto.DeleteAccountRequest;
import com.koinonia.backend.user.dto.UpdateProfileRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegressionSmokeTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void fullLifecycleSmoke() throws Exception {

        // ── 1. Register two users ─────────────────────────────────────────────
        String tokenA = registerAndLogin("smokeA", "smokeA@koinonia.dev", "Password1!");
        String tokenB = registerAndLogin("smokeB", "smokeB@koinonia.dev", "Password1!");

        // ── 2. Update A's profile ─────────────────────────────────────────────
        UpdateProfileRequest profileUpdate = new UpdateProfileRequest();
        profileUpdate.setDisplayName("Alpha User");
        profileUpdate.setBio("Hello from smoke test");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Alpha User"))
                .andExpect(jsonPath("$.bio").value("Hello from smoke test"));

        // ── 3. A creates a post ───────────────────────────────────────────────
        long postId = createPost(tokenA, "Smoke test post");

        // ── 4. A likes, comments, reposts, favorites the post via B ──────────
        mockMvc.perform(post("/api/v1/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1));

        CreateCommentRequest commentReq = new CreateCommentRequest();
        commentReq.setContent("Great post!");
        mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/posts/" + postId + "/repost")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repostCount").value(1));

        mockMvc.perform(post("/api/v1/posts/" + postId + "/favorite")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoritedByCurrentUser").value(true));

        // ── 5. Verify counts on GET /posts/{id} as B ──────────────────────────
        mockMvc.perform(get("/api/v1/posts/" + postId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.commentCount").value(1))
                .andExpect(jsonPath("$.repostCount").value(1))
                .andExpect(jsonPath("$.likedByCurrentUser").value(true))
                .andExpect(jsonPath("$.repostedByCurrentUser").value(true))
                .andExpect(jsonPath("$.favoritedByCurrentUser").value(true));

        // ── 6. B follows A ────────────────────────────────────────────────────
        long userAId = getUserId(tokenA);
        mockMvc.perform(post("/api/v1/users/" + userAId + "/follow")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerCount").value(1));

        // ── 7. Verify A's profile reflects follower ───────────────────────────
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerCount").value(1))
                .andExpect(jsonPath("$.followingCount").value(0));

        // ── 8. Delete B's account with password confirmation ──────────────────
        DeleteAccountRequest deleteReq = new DeleteAccountRequest();
        deleteReq.setPassword("Password1!");

        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReq)))
                .andExpect(status().isNoContent());

        // ── 9. B's old JWT now returns 401 ────────────────────────────────────
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isUnauthorized());

        // ── 10. Cascade delete removed B's like and repost — counts drop ──────
        mockMvc.perform(get("/api/v1/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.repostCount").value(0))
                .andExpect(jsonPath("$.commentCount").value(0));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

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

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    private long createPost(String token, String content) throws Exception {
        CreatePostRequest req = new CreatePostRequest();
        req.setTopic(Topic.FAITH);
        req.setContent(content);

        MvcResult result = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();
    }

    private long getUserId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();
    }
}
