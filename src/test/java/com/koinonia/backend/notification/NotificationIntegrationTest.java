package com.koinonia.backend.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koinonia.backend.auth.dto.LoginRequest;
import com.koinonia.backend.auth.dto.RegisterRequest;
import com.koinonia.backend.post.dto.CreatePostRequest;
import com.koinonia.backend.comment.dto.CreateCommentRequest;
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
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificationIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void notificationLifecycle_emitAndMarkAsRead() throws Exception {
        // 1. Register and login as users A and B
        String tokenA = registerAndLogin("userA", "userA@koinonia.dev", "Password1");
        String tokenB = registerAndLogin("userB", "userB@koinonia.dev", "Password1");

        // 2. User A creates a post
        CreatePostRequest postReq = new CreatePostRequest();
        postReq.setContent("Hello from user A, this is a test post");

        MvcResult createPostResult = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long postId = objectMapper.readTree(createPostResult.getResponse().getContentAsString())
                .get("id").asLong();

        // 3. User B likes user A's post → expect a LIKE notification for A
        mockMvc.perform(post("/api/v1/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());

        // Verify A has 1 unread notification
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        // 4. User B unlikes, then likes again
        mockMvc.perform(delete("/api/v1/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());

        // Verify still 1 unread notification (deduplication within 24h)
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        // 5. User B comments on user A's post → expect a COMMENT notification for A
        CreateCommentRequest commentReq = new CreateCommentRequest();
        commentReq.setContent("Great post! This is a thoughtful comment");

        mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReq)))
                .andExpect(status().isCreated());

        // Verify A now has 2 unread notifications
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));

        // 6. User A gets all notifications (should be 2: LIKE and COMMENT, newest first)
        MvcResult notifResult = mockMvc.perform(get("/api/v1/notifications?page=0&size=20")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].type").value("COMMENT"))
                .andExpect(jsonPath("$.content[0].actor.username").value("userB"))
                .andExpect(jsonPath("$.content[0].commentPreview").value("Great post! This is a thoughtful comment"))
                .andExpect(jsonPath("$.content[0].readAt").isEmpty())
                .andExpect(jsonPath("$.content[1].type").value("LIKE"))
                .andExpect(jsonPath("$.content[1].actor.username").value("userB"))
                .andExpect(jsonPath("$.content[1].post.id").value(postId))
                .andExpect(jsonPath("$.content[1].readAt").isEmpty())
                .andReturn();

        long notifIdComment = objectMapper.readTree(notifResult.getResponse().getContentAsString())
                .get("content").get(0).get("id").asLong();
        long notifIdLike = objectMapper.readTree(notifResult.getResponse().getContentAsString())
                .get("content").get(1).get("id").asLong();

        // 7. User A marks all notifications as read
        MvcResult markAllResult = mockMvc.perform(post("/api/v1/notifications/mark-all-read")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        long markedRead = objectMapper.readTree(markAllResult.getResponse().getContentAsString())
                .get("markedRead").asLong();
        assert markedRead == 2L : "Expected 2 marked as read, got " + markedRead;

        // 8. Verify unread-count is now 0
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));

        // 9. User B follows user A → expect a FOLLOW notification for A
        mockMvc.perform(post("/api/v1/users/" + getUserIdFromToken(tokenA) + "/follow")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());

        // Verify A now has 1 unread notification (the FOLLOW)
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        // 10. Get notifications and verify FOLLOW is there with post=null
        mockMvc.perform(get("/api/v1/notifications?page=0&size=20")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].type").value("FOLLOW"))
                .andExpect(jsonPath("$.content[0].actor.username").value("userB"))
                .andExpect(jsonPath("$.content[0].post").isEmpty());

        // 11. User B tries to mark user A's LIKE notification as read → expect 403 (forbidden)
        mockMvc.perform(post("/api/v1/notifications/" + notifIdLike + "/read")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // 12. User B tries to mark a non-existent notification → expect 404
        mockMvc.perform(post("/api/v1/notifications/99999/read")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // 13. User A marks the FOLLOW notification as read → expect 200 with ok=true
        mockMvc.perform(post("/api/v1/notifications/" + notifIdComment + "/read")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        // 14. Re-mark as read is idempotent
        mockMvc.perform(post("/api/v1/notifications/" + notifIdComment + "/read")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        // 15. Self-test: User A likes their own post → no notification created
        mockMvc.perform(post("/api/v1/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Verify notification count for A hasn't changed (still 1 from FOLLOW)
        mockMvc.perform(get("/api/v1/notifications?page=0&size=20")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3))); // Still 3 total (LIKE, COMMENT, FOLLOW)
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

    private Long getUserIdFromToken(String token) throws Exception {
        // Extract user ID from the JWT payload
        String[] parts = token.split("\\.");
        String payload = parts[1];
        String decoded = new String(java.util.Base64.getDecoder().decode(payload));
        return objectMapper.readTree(decoded).get("sub").asLong();
    }
}
