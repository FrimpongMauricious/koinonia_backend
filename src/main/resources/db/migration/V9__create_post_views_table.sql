CREATE TABLE post_views (
    id        BIGSERIAL PRIMARY KEY,
    user_id   BIGINT NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    post_id   BIGINT NOT NULL REFERENCES posts(id)  ON DELETE CASCADE,
    viewed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, post_id)
);

CREATE INDEX idx_post_views_post_id ON post_views (post_id);
