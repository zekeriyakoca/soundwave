CREATE TABLE outbox_events (
                               id             CHAR(36)     PRIMARY KEY,
                               aggregate_type VARCHAR(50)  NOT NULL,
                               aggregate_id   CHAR(36)     NOT NULL,
                               event_type     VARCHAR(100) NOT NULL,
                               payload        JSON         NOT NULL,
                               published      BOOLEAN      NOT NULL DEFAULT FALSE,
                               created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               published_at   TIMESTAMP    NULL,
                               version        BIGINT       NOT NULL DEFAULT 0,

                               INDEX idx_outbox_published_created_at (published, created_at),
                               INDEX idx_outbox_aggregate (aggregate_type, aggregate_id)
);