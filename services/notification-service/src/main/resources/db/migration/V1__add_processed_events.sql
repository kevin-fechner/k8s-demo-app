CREATE TABLE processed_events
(
    id           BIGSERIAL PRIMARY KEY,
    event_id     VARCHAR(255) NOT NULL,
    event_type   VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP    NOT NULL,
    CONSTRAINT uq_processed_events UNIQUE (event_id, event_type)
);