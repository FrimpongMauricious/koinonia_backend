CREATE TABLE user_streaks (
    user_id         BIGINT    PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    current_streak  INT       NOT NULL DEFAULT 0,
    longest_streak  INT       NOT NULL DEFAULT 0,
    last_activity_date DATE,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
