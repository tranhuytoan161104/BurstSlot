CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE slot (
    event_id BIGINT PRIMARY KEY REFERENCES events(id),
    available_quantity INTEGER NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_positive_slot CHECK (available_quantity >= 0)
);

CREATE INDEX idx_slot_event_id ON slot(event_id);

CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL REFERENCES events(id),
    quantity INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reservations_user_event ON reservations(user_id, event_id);

CREATE INDEX idx_reservations_idempotency ON reservations(idempotency_key);

CREATE TABLE IF NOT EXISTS outbox_events (
    id SERIAL PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

INSERT INTO events (name, start_time, end_time, created_at) VALUES 
('Rock Concert 2026', '2026-05-10T19:00:00+07:00', '2026-05-10T23:00:00+07:00', NOW()),
('Tech Workshop: Spring Boot', '2026-06-15T09:00:00+07:00', '2026-06-15T12:00:00+07:00', NOW()),
('Flash Sale: BurstSlot Pro', '2026-07-01T00:00:00+07:00', '2026-07-01T01:00:00+07:00', NOW());

INSERT INTO slot (event_id, available_quantity, version) VALUES 
(1, 500, 0),
(2, 50, 0),
(3, 10, 0);