package com.koinonia.backend.user;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserProfileIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void publicProfileLookup() throws Exception {
        // 1. Register A and B
        registerAndGetId("profileA", "profileA@koinonia.dev", "Password1!");
        long userBId = registerAndGetId("profileB", "profileB@koinonia.dev", "Password1!");
        String tokenA = login("profileA@koinonia.dev", "Password1!");
        String tokenB = login("profileB@koinonia.dev", "Password1!");

        // 2. A follows B
        mockMvc.perform(post("/api/v1/users/" + userBId + "/follow")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // 3. Anonymous GET /users/{B} → 200, followedByCurrentUser=false, followerCount=1
        mockMvc.perform(get("/api/v1/users/" + userBId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userBId))
                .andExpect(jsonPath("$.username").value("profileB"))
                .andExpect(jsonPath("$.followerCount").value(1))
                .andExpect(jsonPath("$.followedByCurrentUser").value(false))
                .andExpect(jsonPath("$.email").doesNotExist());

        // 4. As A → followedByCurrentUser=true
        mockMvc.perform(get("/api/v1/users/" + userBId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerCount").value(1))
                .andExpect(jsonPath("$.followedByCurrentUser").value(true));

        // 5. As B (self-lookup) → followedByCurrentUser=false
        mockMvc.perform(get("/api/v1/users/" + userBId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerCount").value(1))
                .andExpect(jsonPath("$.followedByCurrentUser").value(false));

        // 6. Unknown user → 404
        mockMvc.perform(get("/api/v1/users/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // 7. /me without token still requires auth → 401
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private long registerAndGetId(String username, String email, String password) throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername(username);
        reg.setEmail(email);
        reg.setPassword(password);
        reg.setDisplayName(username);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("user").get("id").asLong();
    }

    private String login(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmailOrUsername(email);
        req.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }
}
