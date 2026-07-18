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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TopicFeedTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void topicFilteringAndCounts() throws Exception {
        String token = registerAndLogin("topicUser", "topicUser@koinonia.dev", "Password1!");

        createPost(token, "Faith post 1", "FAITH");
        createPost(token, "Faith post 2", "FAITH");
        createPost(token, "Prayer post", "PRAYER");

        // Filtered feed: FAITH → 2 posts
        mockMvc.perform(get("/api/v1/posts?topic=FAITH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        // Filtered feed: PRAYER → 1 post
        mockMvc.perform(get("/api/v1/posts?topic=PRAYER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // Unfiltered feed → 3 posts
        mockMvc.perform(get("/api/v1/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        // Topics endpoint returns wrapped shape with exactly 8 entries (no GENERAL)
        mockMvc.perform(get("/api/v1/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics").isArray())
                .andExpect(jsonPath("$.topics", hasSize(8)))
                // FAITH is first in enum order and has postCount 2
                .andExpect(jsonPath("$.topics[0].topic").value("FAITH"))
                .andExpect(jsonPath("$.topics[0].displayName").value("Faith"))
                .andExpect(jsonPath("$.topics[0].postCount").value(2))
                // PRAYER is second, postCount 1
                .andExpect(jsonPath("$.topics[1].topic").value("PRAYER"))
                .andExpect(jsonPath("$.topics[1].displayName").value("Prayer"))
                .andExpect(jsonPath("$.topics[1].postCount").value(1))
                // GENERAL must not appear anywhere in the list
                .andExpect(jsonPath("$.topics[*].topic", not(hasItem("GENERAL"))));

        // Each post in the filtered feed carries topic and content
        mockMvc.perform(get("/api/v1/posts?topic=PRAYER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].topic").value("PRAYER"))
                .andExpect(jsonPath("$.content[0].content").value("Prayer post"));
    }

    @Test
    void generalTopicRejectedOnCreate() throws Exception {
        String token = registerAndLogin("genUser", "genUser@koinonia.dev", "Password1!");

        mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("content", "test", "topic", "GENERAL"))))
                .andExpect(status().isBadRequest());
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

    private void createPost(String token, String content, String topic) throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", content, "topic", topic))))
                .andExpect(status().isCreated());
    }
}
