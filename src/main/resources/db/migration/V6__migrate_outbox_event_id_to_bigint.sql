CREATE TABLE outbox_events_v2 (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    aggregate_type VARCHAR(50)  NOT NULL,
    aggregate_id   CHAR(36)     NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        JSON         NOT NULL,
    published      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at   TIMESTAMP    NULL,
    version        BIGINT       NOT NULL DEFAULT 0,

    INDEX idx_outbox_published_created_at_id (published, created_at, id),
    INDEX idx_outbox_aggregate (aggregate_type, aggregate_id)
);

INSERT INTO outbox_events_v2 (
    aggregate_type,
    aggregate_id,
    event_type,
    payload,
    published,
    created_at,
    published_at,
    version
)
SELECT
    aggregate_type,
    aggregate_id,
    event_type,
    payload,
    published,
    created_at,
    published_at,
    version
FROM outbox_events
ORDER BY created_at, id;

DROP TABLE outbox_events;

RENAME TABLE outbox_events_v2 TO outbox_events;
