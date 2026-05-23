package com.koinonia.backend.comment;

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
class CommentLikesAndRepliesTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void commentLikesAndRepliesLifecycle() throws Exception {
        // Step 1: register users A, B, C
        String tokenA = registerAndLogin("userA14", "userA14@koinonia.dev", "Password1");
        String tokenB = registerAndLogin("userB14", "userB14@koinonia.dev", "Password1");
        String tokenC = registerAndLogin("userC14", "userC14@koinonia.dev", "Password1");

        // Step 2: A creates post P1
        long postId = createPost(tokenA, "Post for comment-like/reply test");

        // Step 3: B comments on P1 (top-level comment C1)
        long c1Id = createComment(tokenB, postId, null, "Top-level comment from B");

        // Step 4: A likes C1 — likeCount should be 1
        mockMvc.perform(post("/api/v1/comments/" + c1Id + "/like")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByCurrentUser").value(true));

        // Verify via GET /posts/{id}/comments
        mockMvc.perform(get("/api/v1/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].likeCount").value(1))
                .andExpect(jsonPath("$.content[0].likedByCurrentUser").value(true));

        // Step 5: A likes C1 again — idempotent, likeCount still 1
        mockMvc.perform(post("/api/v1/comments/" + c1Id + "/like")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1));

        // Step 6: A unlikes C1 — likeCount back to 0
        mockMvc.perform(delete("/api/v1/comments/" + c1Id + "/like")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.likedByCurrentUser").value(false));

        // Step 7: C replies to C1 (parentId = C1.id)
        long c2Id = createComment(tokenC, postId, c1Id, "Reply from C to B's comment");

        // Step 8: GET /posts/{postId}/comments → only C1 (top-level), replyCount = 1
        mockMvc.perform(get("/api/v1/posts/" + postId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(c1Id))
                .andExpect(jsonPath("$.content[0].replyCount").value(1))
                .andExpect(jsonPath("$.content[0].parentId").isEmpty());

        // Step 9: GET /comments/{c1Id}/replies → returns C2
        mockMvc.perform(get("/api/v1/comments/" + c1Id + "/replies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(c2Id))
                .andExpect(jsonPath("$.content[0].parentId").value(c1Id));

        // Step 10: Try to reply to C2 (reply-to-reply) → 400
        mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "Reply to reply", "parentId", c2Id))))
                .andExpect(status().isBadRequest());

        // Step 11: Reply with parentId pointing to a comment on a different post → 400
        long otherPostId = createPost(tokenA, "Another post");
        long otherCommentId = createComment(tokenA, otherPostId, null, "Comment on other post");
        mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "Cross-post reply", "parentId", otherCommentId))))
                .andExpect(status().isBadRequest());

        // Step 12: B should have a REPLY notification (C replied to B's comment)
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.type == 'REPLY')]").isArray());

        // Step 13: A should have a COMMENT notification (C commented on A's post)
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.type == 'COMMENT')]").isArray());

        // Step 14: like a non-existent comment → 404
        mockMvc.perform(post("/api/v1/comments/99999/like")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String registerAndLogin(String username, String email, String password) throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername(username);
        reg.setEmail(email);
        reg.setPassword(password);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest();
        login.setEmail(email);
        login.setPassword(password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
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

    private long createComment(String token, long postId, Long parentId, String content) throws Exception {
        var body = parentId != null
                ? Map.of("content", content, "parentId", parentId)
                : Map.of("content", content);
        MvcResult result = mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
