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
