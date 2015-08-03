CREATE TABLE undelivered_messages
(
  id serial NOT NULL,
  sender text NOT NULL,
  destination text NOT NULL,
  message text,
  sending_time timestamp without time zone,
  CONSTRAINT primary_key PRIMARY KEY (id)
);