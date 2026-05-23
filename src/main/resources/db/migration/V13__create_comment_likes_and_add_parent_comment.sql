-- Comment likes
CREATE TABLE comment_likes (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    comment_id BIGINT    NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, comment_id)
);

CREATE INDEX idx_comment_likes_comment_id ON comment_likes (comment_id);

-- Comment replies (parent reference, one level deep)
ALTER TABLE comments ADD COLUMN parent_id BIGINT REFERENCES comments(id) ON DELETE CASCADE;
CREATE INDEX idx_comments_parent_id ON comments (parent_id);

-- Extend notification type constraint to include COMMENT_LIKE and REPLY
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_type_check CHECK (
    type IN ('LIKE', 'COMMENT', 'REPOST', 'FOLLOW', 'COMMENT_LIKE', 'REPLY')
);
