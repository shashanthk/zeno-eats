-- user-service schema
-- Runs once on first container startup (empty volume).
-- Spring Boot's naming strategy maps camelCase fields → snake_case columns.
-- To re-run: docker compose down -v && docker compose up -d

CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL       PRIMARY KEY,
    email       VARCHAR(255)    NOT NULL,
    password    VARCHAR(255)    NOT NULL,
    first_name  VARCHAR(255)    NOT NULL,
    last_name   VARCHAR(255)    NOT NULL,
    role        VARCHAR(50)     NOT NULL DEFAULT 'CUSTOMER',

    CONSTRAINT uq_users_email   UNIQUE (email),
    CONSTRAINT chk_users_role   CHECK  (role IN ('CUSTOMER', 'ADMIN'))
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);
