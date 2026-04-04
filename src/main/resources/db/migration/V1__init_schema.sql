CREATE TABLE artists (
    id          CHAR(36)     PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    bio         TEXT,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id             CHAR(36)      PRIMARY KEY,
    artist_id      CHAR(36)      NOT NULL,
    title          VARCHAR(255)  NOT NULL,
    upc            VARCHAR(12)   UNIQUE,
    release_date   DATE,
    genre          VARCHAR(50)   NOT NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    price_amount   DECIMAL(10,2),
    price_currency VARCHAR(3)    DEFAULT 'EUR',
    version        BIGINT        NOT NULL DEFAULT 0,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_product_artist FOREIGN KEY (artist_id) REFERENCES artists(id),
    INDEX idx_products_status (status),
    INDEX idx_products_artist (artist_id),
    INDEX idx_products_genre (genre)
);

CREATE TABLE tracks (
    id            CHAR(36)     PRIMARY KEY,
    product_id    CHAR(36)     NOT NULL,
    title         VARCHAR(255) NOT NULL,
    duration_ms   INT          NOT NULL,
    track_number  INT          NOT NULL,
    isrc          VARCHAR(12),
    version       BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT fk_track_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_tracks_product (product_id)
);

CREATE TABLE outbox_events (
    id             BIGINT       AUTO_INCREMENT PRIMARY KEY,
    aggregate_type VARCHAR(50)  NOT NULL,
    aggregate_id   CHAR(36)     NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        JSON         NOT NULL,
    published      BOOLEAN      NOT NULL DEFAULT FALSE,
    failed         BOOLEAN      NOT NULL DEFAULT FALSE,
    failure_reason TEXT         NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at   TIMESTAMP    NULL,
    version        BIGINT       NOT NULL DEFAULT 0,

    INDEX idx_outbox_pending (published, failed, created_at, id),
    INDEX idx_outbox_aggregate (aggregate_type, aggregate_id)
);

CREATE TABLE processed_events (
    event_id       VARCHAR(36)  NOT NULL,
    consumer_group VARCHAR(100) NOT NULL,
    processed_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (event_id, consumer_group)
);
