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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserSearchTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void userSearch_returnsByUsernameSubstring() throws Exception {
        String tokenA = registerAndLogin("searchAlpha", "searchAlpha@koinonia.dev", "Password1!");
        String tokenB = registerAndLogin("searchBeta", "searchBeta@koinonia.dev", "Password1!");
        long userAId = getUserId(tokenA);

        // B follows A — so A has 1 follower
        mockMvc.perform(post("/api/v1/users/" + userAId + "/follow")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());

        // Partial match: "search" returns both users
        mockMvc.perform(get("/api/v1/users/search?query=search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));

        // Precise match: "alpha" returns only A
        mockMvc.perform(get("/api/v1/users/search?query=alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].username").value("searchAlpha"));

        // Ordered by createdAt ASC: A (registered first) should be first
        mockMvc.perform(get("/api/v1/users/search?query=search")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("searchAlpha"))
                .andExpect(jsonPath("$.content[0].followerCount").value(1));

        // Anonymous search works too
        mockMvc.perform(get("/api/v1/users/search?query=search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].followedByCurrentUser").value(false));

        // No results
        mockMvc.perform(get("/api/v1/users/search?query=nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
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
}
