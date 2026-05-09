-- Slice 9: in-app notifications
CREATE TABLE IF NOT EXISTS notifications (
    id              BIGSERIAL       PRIMARY KEY,
    recipient_id    BIGINT          NOT NULL,
    actor_id        BIGINT          NOT NULL,
    type            VARCHAR(20)     NOT NULL,
    post_id         BIGINT,
    comment_id      BIGINT,
    read_at         TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_actor FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_comment FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE SET NULL,
    CONSTRAINT check_notification_type CHECK (type IN ('LIKE', 'COMMENT', 'REPOST', 'FOLLOW')),
    CONSTRAINT check_not_self_notification CHECK (recipient_id <> actor_id)
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_created 
    ON notifications (recipient_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_unread 
    ON notifications (recipient_id) WHERE read_at IS NULL;
