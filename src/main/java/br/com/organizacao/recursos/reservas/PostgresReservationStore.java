package br.com.organizacao.recursos.reservas;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
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
                WHERE room_id = ? AND tstzrange(starts_at, ends_at) && tstzrange(?, ?)
            )
            """;
    private static final String ROOM_AND_PROFESSOR_AVAILABILITY_SQL = """
            SELECT NOT EXISTS (
                SELECT 1 FROM reservations
                WHERE (room_id = ? OR professor_id = ?)
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
        List.of("professor:" + professorId, "room:" + roomId).stream()
                .sorted()
                .forEach(this::acquireTransactionLock);
        if (!isAvailable(roomId, professorId, period)) {
            return false;
        }

        try {
            jdbcTemplate.update(
                    "INSERT INTO reservations (room_id, professor_id, starts_at, ends_at) VALUES (?, ?, ?, ?)",
                    roomId,
                    professorId,
                    Timestamp.from(period.startsAt()),
                    Timestamp.from(period.endsAt()));
            return true;
        } catch (DataAccessException exception) {
            if (hasSqlState(exception, "23P01") || hasSqlState(exception, "40P01")) {
                return false;
            }
            throw exception;
        }
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
}
