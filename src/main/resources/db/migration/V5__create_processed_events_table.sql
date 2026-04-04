CREATE TABLE processed_events (
    event_id       CHAR(36)     NOT NULL,
    consumer_group VARCHAR(100) NOT NULL,
    processed_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (event_id, consumer_group)
);
