// Happy-path integration test: register → login → /me.
// Uses @Transactional so each test rolls back — you can run this repeatedly without cleaning the DB.
// Requires PostgreSQL running on localhost:5433 with database koinonia_db (same as dev).
package com.koinonia.backend.auth;

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
@Transactional // rolls back after each test — safe to run repeatedly
class AuthIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void registerAndLogin_returnsJwtAndUserProfile() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("testuser");
        reg.setEmail("test@koinonia.dev");
        reg.setPassword("Password1");
        reg.setDisplayName("Test User");

        // 1. Register — expect 201 with a token
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andReturn();

        // 2. Login — expect 200 with a token
        LoginRequest login = new LoginRequest();
        login.setEmail("test@koinonia.dev");
        login.setPassword("Password1");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        // Extract token from login response
        String body = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(body).get("token").asText();

        // 3. GET /me with the token — expect the correct username back
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@koinonia.dev"));
    }

    @Test
    void register_withDuplicateEmail_returns409() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("user1");
        reg.setEmail("dup@koinonia.dev");
        reg.setPassword("Password1");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        // Flush so the unique constraint fires before rollback
        reg.setUsername("user2"); // different username, same email
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
