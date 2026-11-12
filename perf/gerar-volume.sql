-- Carga de volume para o cenário B do teste de desempenho (docs/performance/rnf.md).
-- Execute com o banco do perfil perf/postgres já inicializado (o schema é criado ao subir a aplicação):
--   PGPASSWORD=<senha> psql -h 127.0.0.1 -U <usuario> -d resource_reservations -f perf/gerar-volume.sql
-- Cria 200.000 reservas sem sobreposição (2.000 salas x 100 faixas de 2 h; um professor por reserva)
-- e 50.000 tentativas recusadas. Somente dados fictícios.
INSERT INTO reservations (room_id, professor_id, requester_id, starts_at, ends_at, status)
SELECT 'seed-room-' || (g % 2000),
       'seed-prof-' || g,
       'seed-user-' || (g % 500),
       timestamptz '2027-01-01 00:00:00+00' + ((g / 2000) * interval '2 hours'),
       timestamptz '2027-01-01 00:00:00+00' + ((g / 2000) * interval '2 hours') + interval '1 hour',
       CASE WHEN g % 10 = 0 THEN 'CANCELADA' ELSE 'AGENDADA' END
FROM generate_series(1, 200000) g;

INSERT INTO reservation_conflict_attempts (room_id, professor_id, starts_at, ends_at)
SELECT 'seed-room-' || (g % 2000), 'seed-prof-x',
       timestamptz '2027-01-01 00:00:00+00', timestamptz '2027-01-01 01:00:00+00'
FROM generate_series(1, 50000) g;

ANALYZE;
