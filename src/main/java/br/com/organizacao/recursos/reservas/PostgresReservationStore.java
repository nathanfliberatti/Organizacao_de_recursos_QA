package br.com.organizacao.recursos.reservas;

import java.sql.SQLException;
import java.sql.Timestamp;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class PostgresReservationStore implements ReservationStore {
    private final JdbcTemplate jdbcTemplate;

    public PostgresReservationStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean schedule(String roomId, String professorId, String requesterId, ReservationPeriod period) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO reservations (room_id, professor_id, starts_at, ends_at) VALUES (?, ?, ?, ?)",
                    roomId,
                    professorId,
                    Timestamp.from(period.startsAt()),
                    Timestamp.from(period.endsAt()));
            return true;
        } catch (DataIntegrityViolationException exception) {
            if (hasSqlState(exception, "23P01")) {
                return false;
            }
            throw exception;
        }
    }

    @Override
    public boolean isAvailable(String roomId, String professorId, ReservationPeriod period) {
        String resourcePredicate = professorId == null
            ? "room_id = ?"
            : "(room_id = ? OR professor_id = ?)";
        String sql = "SELECT NOT EXISTS (SELECT 1 FROM reservations WHERE " + resourcePredicate
            + " AND tstzrange(starts_at, ends_at) && tstzrange(?, ?))";
        Boolean available = professorId == null
            ? jdbcTemplate.queryForObject(
                sql,
                Boolean.class,
                roomId,
                Timestamp.from(period.startsAt()),
                Timestamp.from(period.endsAt()))
            : jdbcTemplate.queryForObject(
                sql,
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
}
