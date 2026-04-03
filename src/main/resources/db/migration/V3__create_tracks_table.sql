CREATE TABLE tracks (
    id            CHAR(36)     PRIMARY KEY,
    product_id    CHAR(36)     NOT NULL,
    title         VARCHAR(255) NOT NULL,
    duration_ms   INT          NOT NULL,
    track_number  INT          NOT NULL,
    isrc          VARCHAR(12),

    CONSTRAINT fk_track_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_tracks_product (product_id)
);
