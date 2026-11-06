package br.com.organizacao.recursos.reservas;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("postgres")
public class PostgresReservationStore implements ReservationStore {
    private static final String ROOM_AVAILABILITY_SQL = """
            SELECT NOT EXISTS (
                SELECT 1 FROM reservations
                WHERE room_id = ? AND status <> 'CANCELADA'
                    AND tstzrange(starts_at, ends_at) && tstzrange(?, ?)
            )
            """;
    private static final String ROOM_AND_PROFESSOR_AVAILABILITY_SQL = """
            SELECT NOT EXISTS (
                SELECT 1 FROM reservations
                WHERE (room_id = ? OR professor_id = ?)
                    AND status <> 'CANCELADA'
                    AND tstzrange(starts_at, ends_at) && tstzrange(?, ?)
            )
            """;

    private final JdbcTemplate jdbcTemplate;

    public PostgresReservationStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public boolean schedule(String roomId, String professorId, String requesterId, ReservationPeriod period) {
        return scheduleWithId(roomId, professorId, requesterId, period).isPresent();
    }

    @Override
    @Transactional
    public OptionalLong scheduleWithId(
            String roomId, String professorId, String requesterId, ReservationPeriod period) {
        List.of("professor:" + professorId, "room:" + roomId).stream()
                .sorted()
                .forEach(this::acquireTransactionLock);
        if (!isAvailable(roomId, professorId, period)) {
            return OptionalLong.empty();
        }

        try {
            Long id = jdbcTemplate.queryForObject("""
                    INSERT INTO reservations (room_id, professor_id, requester_id, starts_at, ends_at)
                    VALUES (?, ?, ?, ?, ?)
                    RETURNING id
                    """,
                    Long.class,
                    roomId,
                    professorId,
                    requesterId,
                    Timestamp.from(period.startsAt()),
                    Timestamp.from(period.endsAt()));
            return OptionalLong.of(java.util.Objects.requireNonNull(id));
        } catch (DataAccessException exception) {
            if (hasSqlState(exception, "23P01") || hasSqlState(exception, "40P01")) {
                return OptionalLong.empty();
            }
            throw exception;
        }
    }

    @Override
    @Transactional
    public void recordConflictAttempt(String roomId, String professorId, ReservationPeriod period) {
        jdbcTemplate.update("""
                INSERT INTO reservation_conflict_attempts (room_id, professor_id, starts_at, ends_at)
                VALUES (?, ?, ?, ?)
                """,
                roomId,
                professorId,
                Timestamp.from(period.startsAt()),
                Timestamp.from(period.endsAt()));
    }

    @Override
    public boolean isAvailable(String roomId, String professorId, ReservationPeriod period) {
        Boolean available = professorId == null
            ? jdbcTemplate.queryForObject(
                ROOM_AVAILABILITY_SQL,
                Boolean.class,
                roomId,
                Timestamp.from(period.startsAt()),
                Timestamp.from(period.endsAt()))
            : jdbcTemplate.queryForObject(
                ROOM_AND_PROFESSOR_AVAILABILITY_SQL,
                Boolean.class,
                roomId,
                professorId,
                Timestamp.from(period.startsAt()),
                Timestamp.from(period.endsAt()));
        return Boolean.TRUE.equals(available);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> requesterId(long reservationId) {
        List<String> requesterIds = jdbcTemplate.query(
                "SELECT requester_id FROM reservations WHERE id = ? AND status = 'AGENDADA'",
                (resultSet, rowNumber) -> resultSet.getString(1),
                reservationId);
        return requesterIds.stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduledReservation> findByRequesterId(String requesterId) {
        return jdbcTemplate.query("""
                SELECT id, room_id, professor_id, requester_id, starts_at, ends_at
                FROM reservations
                WHERE requester_id = ? AND status <> 'CANCELADA'
                ORDER BY starts_at, id
                """,
                (resultSet, rowNumber) -> new ScheduledReservation(
                        resultSet.getLong("id"),
                        resultSet.getString("room_id"),
                        resultSet.getString("professor_id"),
                        resultSet.getString("requester_id"),
                        resultSet.getTimestamp("starts_at").toInstant(),
                        resultSet.getTimestamp("ends_at").toInstant()),
                requesterId);
    }

    @Override
    @Transactional
    public void update(long reservationId, ReservationPeriod period) {
        List<ReservationResourceIdentity> matches = jdbcTemplate.query("""
                SELECT room_id, professor_id
                FROM reservations
                WHERE id = ? AND status = 'AGENDADA'
                FOR UPDATE
                """,
                (resultSet, rowNumber) -> new ReservationResourceIdentity(
                        resultSet.getString("room_id"), resultSet.getString("professor_id")),
                reservationId);
        if (matches.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Reservation not found");
        }

        ReservationResourceIdentity reservation = matches.getFirst();
        List.of("professor:" + reservation.professorId(), "room:" + reservation.roomId()).stream()
                .sorted()
                .forEach(this::acquireTransactionLock);
        Boolean available = jdbcTemplate.queryForObject("""
                SELECT NOT EXISTS (
                    SELECT 1 FROM reservations
                    WHERE id <> ? AND (room_id = ? OR professor_id = ?)
                        AND status <> 'CANCELADA'
                        AND tstzrange(starts_at, ends_at) && tstzrange(?, ?)
                )
                """,
                Boolean.class,
                reservationId,
                reservation.roomId(),
                reservation.professorId(),
                Timestamp.from(period.startsAt()),
                Timestamp.from(period.endsAt()));
        if (!Boolean.TRUE.equals(available)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "Reservation period overlaps another reservation");
        }

        try {
            jdbcTemplate.update("""
                    UPDATE reservations SET starts_at = ?, ends_at = ?
                    WHERE id = ? AND status = 'AGENDADA'
                    """,
                    Timestamp.from(period.startsAt()),
                    Timestamp.from(period.endsAt()),
                    reservationId);
        } catch (DataAccessException exception) {
            if (hasSqlState(exception, "23P01") || hasSqlState(exception, "40P01")) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "Reservation period overlaps another reservation",
                        exception);
            }
            throw exception;
        }
    }

    private boolean hasSqlState(Throwable throwable, String expectedState) {
        Throwable cause = throwable;
        while (cause != null) {
            if (cause instanceof SQLException sqlException && expectedState.equals(sqlException.getSQLState())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private void acquireTransactionLock(String lockKey) {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))")) {
                statement.setString(1, lockKey);
                statement.execute();
            }
            return null;
        });
    }

    private record ReservationResourceIdentity(String roomId, String professorId) {
    }
}
