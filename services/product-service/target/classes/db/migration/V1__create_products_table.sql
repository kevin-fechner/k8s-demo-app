CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    price       NUMERIC(10, 2) NOT NULL,
    stock       INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO products (name, description, price, stock) VALUES
    ('Laptop',     'High performance laptop',    999.99, 50),
    ('Mouse',      'Wireless ergonomic mouse',    29.99, 200),
    ('Keyboard',   'Mechanical keyboard',         79.99, 150),
    ('Monitor',    '4K Ultra HD monitor',        399.99, 75),
    ('Headphones', 'Noise cancelling headphones', 149.99, 100);