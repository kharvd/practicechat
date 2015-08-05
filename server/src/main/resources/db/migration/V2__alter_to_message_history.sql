ALTER TABLE undelivered_messages RENAME TO messages;
ALTER TABLE messages ADD COLUMN delivered boolean NOT NULL DEFAULT FALSE;