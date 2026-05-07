package com.koinonia.backend.repost;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koinonia.backend.auth.dto.LoginRequest;
import com.koinonia.backend.auth.dto.RegisterRequest;
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
class RepostFavoriteIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void repostAndFavoriteLifecycle() throws Exception {
        // 1. Register A and B; A creates a post
        String tokenA = registerAndLogin("repostUserA", "repostA@koinonia.dev", "Password1");
        String tokenB = registerAndLogin("repostUserB", "repostB@koinonia.dev", "Password1");
        long postId   = createPost(tokenA, "Hello from A");

        // 2. A tries to repost own post → 400
        mockMvc.perform(post("/api/v1/posts/" + postId + "/repost")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot repost your own post"));

        // 3. B reposts A's post → repostCount=1, repostedByCurrentUser=true
        mockMvc.perform(post("/api/v1/posts/" + postId + "/repost")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repostCount").value(1))
                .andExpect(jsonPath("$.repostedByCurrentUser").value(true));

        // 4. B reposts again (idempotent) → still repostCount=1
        mockMvc.perform(post("/api/v1/posts/" + postId + "/repost")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repostCount").value(1));

        // 5. B un-reposts → repostCount=0, repostedByCurrentUser=false
        mockMvc.perform(delete("/api/v1/posts/" + postId + "/repost")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repostCount").value(0))
                .andExpect(jsonPath("$.repostedByCurrentUser").value(false));

        // 6. B un-reposts again (idempotent) → no error
        mockMvc.perform(delete("/api/v1/posts/" + postId + "/repost")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repostCount").value(0));

        // 7. B favorites the post → favoritedByCurrentUser=true
        mockMvc.perform(post("/api/v1/posts/" + postId + "/favorite")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoritedByCurrentUser").value(true));

        // 8. B favorites again (idempotent) → still favoritedByCurrentUser=true
        mockMvc.perform(post("/api/v1/posts/" + postId + "/favorite")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoritedByCurrentUser").value(true));

        // 9. GET /users/me/favorites as B → 1 item total
        mockMvc.perform(get("/api/v1/users/me/favorites")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // 10. B un-favorites → favoritedByCurrentUser=false
        mockMvc.perform(delete("/api/v1/posts/" + postId + "/favorite")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoritedByCurrentUser").value(false));

        // 11. B un-favorites again (idempotent) → no error, favoritedByCurrentUser=false
        mockMvc.perform(delete("/api/v1/posts/" + postId + "/favorite")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoritedByCurrentUser").value(false));
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

        return login(email, password);
    }

    private String login(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    private long createPost(String token, String content) throws Exception {
        CreatePostRequest req = new CreatePostRequest();
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
}
