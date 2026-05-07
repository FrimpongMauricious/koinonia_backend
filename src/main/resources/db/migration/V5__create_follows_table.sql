CREATE TABLE follows (
    id           BIGSERIAL PRIMARY KEY,
    follower_id  BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    following_id BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_follows_pair     UNIQUE (follower_id, following_id),
    CONSTRAINT chk_follows_no_self CHECK  (follower_id != following_id)
);

CREATE INDEX idx_follows_follower_id  ON follows(follower_id);
CREATE INDEX idx_follows_following_id ON follows(following_id);
