CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE notebook (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(256) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_edit_time TIMESTAMP NOT NULL,
    status VARCHAR(64) NOT NULL
);