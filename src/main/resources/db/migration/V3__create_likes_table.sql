CREATE TABLE post_likes (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id    BIGINT    NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_post_likes_user_post UNIQUE (user_id, post_id)
);

CREATE INDEX idx_post_likes_post_id ON post_likes(post_id);
