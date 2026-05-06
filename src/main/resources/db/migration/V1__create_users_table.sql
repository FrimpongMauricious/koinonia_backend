-- Slice 1: auth foundation
-- BIGSERIAL gives us auto-incrementing Long PKs (maps to GenerationType.IDENTITY in JPA).
-- Flyway is the single source of truth for schema changes; Hibernate only validates.

CREATE TABLE IF NOT EXISTS users (
    id                  BIGSERIAL       PRIMARY KEY,
    username            VARCHAR(50)     UNIQUE NOT NULL,
    email               VARCHAR(255)    UNIQUE NOT NULL,
    password_hash       VARCHAR(255)    NOT NULL,
    display_name        VARCHAR(100),
    bio                 VARCHAR(500),
    profile_picture_url VARCHAR(500),
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for the two most common lookup paths: login (by email) and JWT validation (by username)
CREATE INDEX IF NOT EXISTS idx_users_email    ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);
