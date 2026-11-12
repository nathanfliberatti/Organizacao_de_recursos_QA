CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE IF NOT EXISTS resources (
    resource_id TEXT PRIMARY KEY,
    capacity INTEGER NOT NULL CHECK (capacity > 0)
);

CREATE TABLE IF NOT EXISTS reservations (
    id BIGSERIAL PRIMARY KEY,
    room_id TEXT NOT NULL,
    professor_id TEXT NOT NULL,
    requester_id TEXT NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    status TEXT NOT NULL DEFAULT 'AGENDADA',
    CONSTRAINT reservations_valid_period CHECK (ends_at > starts_at),
    CONSTRAINT reservations_room_no_overlap
        EXCLUDE USING gist (room_id WITH =, tstzrange(starts_at, ends_at) WITH &&)
        WHERE (status <> 'CANCELADA'),
    CONSTRAINT reservations_professor_no_overlap
        EXCLUDE USING gist (professor_id WITH =, tstzrange(starts_at, ends_at) WITH &&)
        WHERE (status <> 'CANCELADA')
);

ALTER TABLE reservations ADD COLUMN IF NOT EXISTS status TEXT NOT NULL DEFAULT 'AGENDADA';
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS requester_id TEXT;
UPDATE reservations SET requester_id = '__legacy_unassigned__:' || id::TEXT WHERE requester_id IS NULL;
ALTER TABLE reservations ALTER COLUMN requester_id SET NOT NULL;
ALTER TABLE reservations DROP CONSTRAINT IF EXISTS reservations_room_no_overlap;
ALTER TABLE reservations DROP CONSTRAINT IF EXISTS reservations_professor_no_overlap;
ALTER TABLE reservations ADD CONSTRAINT reservations_room_no_overlap
    EXCLUDE USING gist (room_id WITH =, tstzrange(starts_at, ends_at) WITH &&)
    WHERE (status <> 'CANCELADA');
ALTER TABLE reservations ADD CONSTRAINT reservations_professor_no_overlap
    EXCLUDE USING gist (professor_id WITH =, tstzrange(starts_at, ends_at) WITH &&)
    WHERE (status <> 'CANCELADA');
ALTER TABLE reservations DROP CONSTRAINT IF EXISTS reservations_status_check;
ALTER TABLE reservations ADD CONSTRAINT reservations_status_check
    CHECK (status IN ('AGENDADA', 'CANCELADA'));

CREATE TABLE IF NOT EXISTS reservation_conflict_attempts (
    attempt_id BIGSERIAL PRIMARY KEY,
    room_id TEXT NOT NULL,
    professor_id TEXT NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    rejected_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT reservation_conflict_attempts_valid_period CHECK (ends_at > starts_at)
);

CREATE TABLE IF NOT EXISTS reservation_lifecycle (
    reservation_id TEXT PRIMARY KEY,
    requester_id TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('SOLICITADA', 'APROVADA', 'EM_USO', 'CONCLUIDA', 'REJEITADA', 'CANCELADA', 'NAO_COMPARECEU')),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS reservation_audit (
    audit_id BIGSERIAL PRIMARY KEY,
    reservation_id TEXT NOT NULL REFERENCES reservation_lifecycle (reservation_id) ON DELETE RESTRICT,
    previous_status TEXT,
    new_status TEXT NOT NULL CHECK (new_status IN ('SOLICITADA', 'APROVADA', 'EM_USO', 'CONCLUIDA', 'REJEITADA', 'CANCELADA', 'NAO_COMPARECEU')),
    user_id TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    reason TEXT NOT NULL CHECK (length(trim(reason)) > 0),
    CONSTRAINT reservation_audit_initial_status
        CHECK (previous_status IS NOT NULL OR new_status = 'SOLICITADA')
);

CREATE INDEX IF NOT EXISTS reservation_audit_reservation_order
    ON reservation_audit (reservation_id, audit_id);

-- ---------------------------------------------------------------------------
-- Usuarios, catalogo de recursos, manutencao, materiais e notificacoes
-- (RF-01, RF-02, RF-03, RF-08, RF-09, RF-11). Todos os comandos sao idempotentes.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS app_users (
    username TEXT PRIMARY KEY,
    password_hash TEXT NOT NULL,
    display_name TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('SOLICITANTE', 'RESPONSAVEL', 'ADMINISTRADOR')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE resources ADD COLUMN IF NOT EXISTS name TEXT;
ALTER TABLE resources ADD COLUMN IF NOT EXISTS resource_type TEXT NOT NULL DEFAULT 'SALA';
ALTER TABLE resources ADD COLUMN IF NOT EXISTS location TEXT;
ALTER TABLE resources ADD COLUMN IF NOT EXISTS competency TEXT;
ALTER TABLE resources ADD COLUMN IF NOT EXISTS restricted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE resources DROP CONSTRAINT IF EXISTS resources_type_check;
ALTER TABLE resources ADD CONSTRAINT resources_type_check
    CHECK (resource_type IN ('SALA', 'PROFESSOR', 'MATERIAL'));
CREATE INDEX IF NOT EXISTS resources_type_idx ON resources (resource_type);

CREATE TABLE IF NOT EXISTS maintenance_periods (
    resource_id TEXT PRIMARY KEY,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT maintenance_periods_valid_period CHECK (ends_at > starts_at)
);

CREATE TABLE IF NOT EXISTS material_loans (
    loan_id BIGSERIAL PRIMARY KEY,
    resource_id TEXT NOT NULL REFERENCES resources (resource_id) ON DELETE RESTRICT,
    borrower_id TEXT NOT NULL,
    registered_by TEXT NOT NULL,
    withdrawn_at TIMESTAMPTZ NOT NULL,
    returned_at TIMESTAMPTZ,
    returned_registered_by TEXT,
    notes TEXT,
    CONSTRAINT material_loans_valid_return CHECK (returned_at IS NULL OR returned_at >= withdrawn_at)
);
ALTER TABLE material_loans ADD COLUMN IF NOT EXISTS returned_registered_by TEXT;
-- No maximo uma retirada em aberto por material.
CREATE UNIQUE INDEX IF NOT EXISTS material_loans_one_open_per_resource
    ON material_loans (resource_id) WHERE returned_at IS NULL;

CREATE TABLE IF NOT EXISTS notifications (
    notification_id BIGSERIAL PRIMARY KEY,
    event_type TEXT NOT NULL CHECK (event_type IN ('CRIACAO', 'ALTERACAO', 'CANCELAMENTO')),
    reservation_id TEXT NOT NULL,
    recipient_id TEXT NOT NULL,
    message TEXT NOT NULL,
    delivery TEXT NOT NULL DEFAULT 'SIMULADA',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS notifications_recipient_idx ON notifications (recipient_id, notification_id DESC);

-- Estados alternativos do ciclo de vida (RF-14): REJEITADA, CANCELADA e NAO_COMPARECEU.
-- As constraints sao recriadas para bancos criados antes desta versao (idempotente).
ALTER TABLE reservation_lifecycle DROP CONSTRAINT IF EXISTS reservation_lifecycle_status_check;
ALTER TABLE reservation_lifecycle ADD CONSTRAINT reservation_lifecycle_status_check
    CHECK (status IN ('SOLICITADA', 'APROVADA', 'EM_USO', 'CONCLUIDA', 'REJEITADA', 'CANCELADA', 'NAO_COMPARECEU'));
ALTER TABLE reservation_audit DROP CONSTRAINT IF EXISTS reservation_audit_new_status_check;
ALTER TABLE reservation_audit ADD CONSTRAINT reservation_audit_new_status_check
    CHECK (new_status IN ('SOLICITADA', 'APROVADA', 'EM_USO', 'CONCLUIDA', 'REJEITADA', 'CANCELADA', 'NAO_COMPARECEU'));
