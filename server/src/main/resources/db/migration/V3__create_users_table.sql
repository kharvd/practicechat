CREATE TABLE users
(
  name text NOT NULL,
  hash text NOT NULL,
  salt text NOT NULL,
  CONSTRAINT users_primary_key PRIMARY KEY (name)
);

DELETE FROM messages;

ALTER TABLE messages
  ADD CONSTRAINT sender_foreign_key FOREIGN KEY (sender) REFERENCES users (name)
   ON UPDATE CASCADE ON DELETE CASCADE;

CREATE INDEX fki_sender_foreign_key
  ON messages(sender);

ALTER TABLE messages
  ADD CONSTRAINT destination_foreign_key FOREIGN KEY (destination) REFERENCES users (name)
   ON UPDATE CASCADE ON DELETE CASCADE;

CREATE INDEX fki_destination_foreign_key
  ON messages(destination);