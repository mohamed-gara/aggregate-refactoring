CREATE TABLE USER_SUBSCRIPTION (
  user_id VARCHAR(100),
  meetup_event_id BIGINT,
  registration_time TIMESTAMP NOT NULL,
  waiting_list BOOLEAN NOT NULL,
  PRIMARY KEY (user_id, meetup_event_id)
);

CREATE SEQUENCE MEETUP_EVENT_ID_SEQ;

