CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE IF NOT EXISTS reservations (
    id BIGSERIAL PRIMARY KEY,
    room_id TEXT NOT NULL,
    professor_id TEXT NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT reservations_valid_period CHECK (ends_at > starts_at),
    CONSTRAINT reservations_room_no_overlap
        EXCLUDE USING gist (room_id WITH =, tstzrange(starts_at, ends_at) WITH &&),
    CONSTRAINT reservations_professor_no_overlap
        EXCLUDE USING gist (professor_id WITH =, tstzrange(starts_at, ends_at) WITH &&)
);

CREATE TABLE IF NOT EXISTS reservation_lifecycle (
    reservation_id TEXT PRIMARY KEY,
    requester_id TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('SOLICITADA', 'APROVADA', 'EM_USO', 'CONCLUIDA')),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS reservation_audit (
    audit_id BIGSERIAL PRIMARY KEY,
    reservation_id TEXT NOT NULL REFERENCES reservation_lifecycle (reservation_id) ON DELETE RESTRICT,
    previous_status TEXT,
    new_status TEXT NOT NULL CHECK (new_status IN ('SOLICITADA', 'APROVADA', 'EM_USO', 'CONCLUIDA')),
    user_id TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    reason TEXT NOT NULL CHECK (length(trim(reason)) > 0),
    CONSTRAINT reservation_audit_initial_status
        CHECK (previous_status IS NOT NULL OR new_status = 'SOLICITADA')
);

CREATE INDEX IF NOT EXISTS reservation_audit_reservation_order
    ON reservation_audit (reservation_id, audit_id);
