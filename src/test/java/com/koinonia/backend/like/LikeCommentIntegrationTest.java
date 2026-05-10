package com.koinonia.backend.like;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koinonia.backend.auth.dto.LoginRequest;
import com.koinonia.backend.auth.dto.RegisterRequest;
import com.koinonia.backend.comment.dto.CreateCommentRequest;
import com.koinonia.backend.post.Topic;
import com.koinonia.backend.post.dto.CreatePostRequest;
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
class LikeCommentIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void likesAndComments_fullLifecycle() throws Exception {
        // 1 & 2. Register and login two users
        String tokenA = registerAndLogin("userAlpha", "alpha@koinonia.dev", "Password1");
        String tokenB = registerAndLogin("userBeta",  "beta@koinonia.dev",  "Password1");

        // 3. User A creates a post
        CreatePostRequest postReq = new CreatePostRequest();
        postReq.setTopic(Topic.FAITH);
        postReq.setContent("A post by user A");

        MvcResult postResult = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long postId = objectMapper.readTree(postResult.getResponse().getContentAsString())
                .get("id").asLong();

        // 4. User B likes the post — expect likeCount=1, likedByCurrentUser=true
        mockMvc.perform(post("/api/v1/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByCurrentUser").value(true));

        // 5. User B likes again (idempotent) — same result, no error
        mockMvc.perform(post("/api/v1/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1));

        // 6. User A fetches feed — sees likeCount=1, likedByCurrentUser=false (A hasn't liked)
        mockMvc.perform(get("/api/v1/posts")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].likeCount").value(1))
                .andExpect(jsonPath("$.content[0].likedByCurrentUser").value(false));

        // 7. User B unlikes — expect likeCount=0
        mockMvc.perform(delete("/api/v1/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.likedByCurrentUser").value(false));

        // 8. User B comments on the post
        CreateCommentRequest commentReq = new CreateCommentRequest();
        commentReq.setContent("Nice post!");

        MvcResult commentResult = mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Nice post!"))
                .andExpect(jsonPath("$.author.username").value("userBeta"))
                .andReturn();

        long commentId = objectMapper.readTree(commentResult.getResponse().getContentAsString())
                .get("id").asLong();

        // 9. User A tries to delete user B's comment — must get 403
        mockMvc.perform(delete("/api/v1/comments/" + commentId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());

        // 10. User B deletes own comment — must succeed with 204
        mockMvc.perform(delete("/api/v1/comments/" + commentId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNoContent());

        // 11. GET comments on post (public) — empty list after deletion
        mockMvc.perform(get("/api/v1/posts/" + postId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

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

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }
}
