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
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_product_artist FOREIGN KEY (artist_id) REFERENCES artists(id),
    INDEX idx_products_status (status),
    INDEX idx_products_artist (artist_id),
    INDEX idx_products_genre (genre)
);
