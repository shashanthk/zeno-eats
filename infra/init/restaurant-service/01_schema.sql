-- restaurant-service schema
-- Runs once on first container startup (empty volume).
-- To re-run: docker compose down -v && ./scripts/infra.sh start

CREATE TABLE IF NOT EXISTS restaurant (
    id      BIGSERIAL       PRIMARY KEY,
    name    VARCHAR(255)    NOT NULL,
    address VARCHAR(255),
    phone   VARCHAR(255),
    active  BOOLEAN         NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS menu_item (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(255),
    description     VARCHAR(255),
    price           DOUBLE PRECISION,
    restaurant_id   BIGINT          NOT NULL,

    CONSTRAINT fk_menu_item_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurant (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_menu_item_restaurant ON menu_item (restaurant_id);
