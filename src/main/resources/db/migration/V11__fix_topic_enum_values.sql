-- Migrate old wrong topic names to the correct enum values
UPDATE posts SET topic = 'SCRIPTURE'    WHERE topic = 'SERMON';
UPDATE posts SET topic = 'ENCOURAGEMENT' WHERE topic = 'DEVOTION';

-- Constrain the column to the 9 valid values
ALTER TABLE posts ADD CONSTRAINT posts_topic_valid CHECK (
    topic IN ('FAITH','PRAYER','WORSHIP','SCRIPTURE','COMMUNITY','TESTIMONY','ENCOURAGEMENT','DOCTRINE','GENERAL')
);
