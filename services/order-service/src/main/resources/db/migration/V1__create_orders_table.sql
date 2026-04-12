CREATE TYPE order_status AS ENUM (
    'PENDING',
    'CONFIRMED',
    'SHIPPED',
    'DELIVERED',
    'CANCELLED'
);

CREATE TABLE orders
(
    id             BIGSERIAL PRIMARY KEY,
    customer_name  VARCHAR(255)   NOT NULL,
    customer_email VARCHAR(255)   NOT NULL,
    status         order_status   NOT NULL DEFAULT 'PENDING',
    total_amount   NUMERIC(10, 2) NOT NULL DEFAULT 0,
    notes          TEXT,
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE order_items
(
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id   BIGINT         NOT NULL,
    product_name VARCHAR(255)   NOT NULL,
    quantity     INTEGER        NOT NULL,
    unit_price   NUMERIC(10, 2) NOT NULL
);