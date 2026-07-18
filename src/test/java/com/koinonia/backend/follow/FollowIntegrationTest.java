package com.koinonia.backend.follow;

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
class FollowIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void followLifecycle_allScenariosIncludingIdempotency() throws Exception {
        // 1. Register A, B, C — capture IDs from register response body
        long userAId = registerAndGetId("userAlpha", "alpha@koinonia.dev", "Password1!");
        long userBId = registerAndGetId("userBeta",  "beta@koinonia.dev",  "Password1!");
        long userCId = registerAndGetId("userGamma", "gamma@koinonia.dev", "Password1!");

        String tokenA = login("alpha@koinonia.dev", "Password1!");
        String tokenC = login("gamma@koinonia.dev", "Password1!");

        // 2. A tries to follow self → 400
        mockMvc.perform(post("/api/v1/users/" + userAId + "/follow")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot follow yourself"));

        // 3. A follows B → followerCount=1, followedByCurrentUser=true
        mockMvc.perform(post("/api/v1/users/" + userBId + "/follow")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerCount").value(1))
                .andExpect(jsonPath("$.followedByCurrentUser").value(true));

        // 4. A follows B again (idempotent) → still followerCount=1, no error
        mockMvc.perform(post("/api/v1/users/" + userBId + "/follow")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerCount").value(1));

        // 5. A follows C
        mockMvc.perform(post("/api/v1/users/" + userCId + "/follow")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // 6. C follows B → B's followerCount=2
        mockMvc.perform(post("/api/v1/users/" + userBId + "/follow")
                        .header("Authorization", "Bearer " + tokenC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerCount").value(2));

        // 7. GET /users/{B}/followers → 2 items total
        mockMvc.perform(get("/api/v1/users/" + userBId + "/followers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        // 8. GET /users/{A}/following → 2 items total (B and C)
        mockMvc.perform(get("/api/v1/users/" + userAId + "/following"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        // 9. GET /users/me as A → followingCount=2, followerCount=0
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followingCount").value(2))
                .andExpect(jsonPath("$.followerCount").value(0))
                .andExpect(jsonPath("$.followedByCurrentUser").value(false));

        // 10. A unfollows B → B's followerCount drops to 1
        mockMvc.perform(delete("/api/v1/users/" + userBId + "/follow")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerCount").value(1));

        // 11. A unfollows B again (idempotent) → no error
        mockMvc.perform(delete("/api/v1/users/" + userBId + "/follow")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerCount").value(1));
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
}
