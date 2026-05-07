package com.koinonia.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koinonia.backend.exception.ApiError;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP-based rate limiting for auth endpoints only.
 * Uses in-memory Bucket4j buckets — sufficient for single-instance (Render free tier).
 * For multi-instance deployments, swap the ConcurrentHashMap for a Redis-backed store
 * using bucket4j-redis or a distributed cache integration.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH    = "/api/v1/auth/login";
    private static final String REGISTER_PATH = "/api/v1/auth/register";

    private final Map<String, Bucket> loginBuckets    = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("POST".equalsIgnoreCase(method)) {
            if (path.equals(LOGIN_PATH)) {
                if (!tryConsume(loginBuckets, clientIp(request), 5)) {
                    rejectWith429(request, response);
                    return;
                }
            } else if (path.equals(REGISTER_PATH)) {
                if (!tryConsume(registerBuckets, clientIp(request), 3)) {
                    rejectWith429(request, response);
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }

    private boolean tryConsume(Map<String, Bucket> buckets, String key, long capacity) {
        Bucket bucket = buckets.computeIfAbsent(key, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(capacity)
                                .refillIntervally(capacity, Duration.ofMinutes(1))
                                .build())
                        .build());
        return bucket.tryConsume(1);
    }

    private void rejectWith429(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiError error = ApiError.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("Too Many Requests")
                .message("Too many requests — please slow down and try again shortly")
                .path(request.getRequestURI())
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
