CREATE TABLE rooms
(
  name text NOT NULL,
  admin text NOT NULL,
  CONSTRAINT rooms_primary_key PRIMARY KEY (name),
  CONSTRAINT room_admin_foreign_key FOREIGN KEY (admin)
      REFERENCES users (name) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE room_members
(
  room text NOT NULL,
  username text NOT NULL,
  CONSTRAINT room_members_pkey PRIMARY KEY (room, username),
  CONSTRAINT room_members_room_foreign_key FOREIGN KEY (room)
      REFERENCES rooms (name) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT room_members_user_foreign_key FOREIGN KEY (username)
      REFERENCES users (name) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE room_messages
(
  id serial NOT NULL,
  sender text NOT NULL,
  room text NOT NULL,
  message text,
  sending_time timestamp without time zone NOT NULL,
  CONSTRAINT id PRIMARY KEY (id),
  CONSTRAINT room_messages_room_foreign_key FOREIGN KEY (room)
      REFERENCES rooms (name) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT room_messages_sender_foreign_key FOREIGN KEY (sender)
      REFERENCES users (name) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX fki_room_messages_room_foreign_key
  ON room_messages(room);

CREATE INDEX fki_room_messages_sender_foreign_key
  ON room_messages(sender);
