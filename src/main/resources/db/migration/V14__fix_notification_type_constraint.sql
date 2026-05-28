-- Drop both possible constraint names so this works whether previous migrations
-- left "check_notification_type" or "notifications_type_check" in place.
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS check_notification_type;
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE notifications ADD CONSTRAINT check_notification_type CHECK (
    type IN ('LIKE', 'COMMENT', 'REPOST', 'FOLLOW', 'COMMENT_LIKE', 'REPLY')
);
