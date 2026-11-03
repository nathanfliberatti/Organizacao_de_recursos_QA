package br.com.organizacao.recursos.reservas;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("postgres")
public class ReservationAuditService {
    private final JdbcTemplate jdbcTemplate;

    public ReservationAuditService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void createReservation(String reservationId, String requesterId, Clock clock, String reason) {
        jdbcTemplate.update(
                "INSERT INTO reservation_lifecycle (reservation_id, requester_id, status) VALUES (?, ?, ?)",
                reservationId,
                requesterId,
                ReservationStatus.SOLICITADA.name());
        insertAudit(new ReservationTransition(
                reservationId,
                null,
                ReservationStatus.SOLICITADA,
                requesterId,
                clock.instant(),
                reason));
    }

    @Transactional
    public ReservationTransition changeState(
            String reservationId,
            ReservationStatus newStatus,
            String userId,
            Clock clock,
            String reason) {
        ReservationStatus previousStatus = lockCurrentStatus(reservationId);
        if (!isAllowedTransition(previousStatus, newStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid reservation state transition");
        }

        ReservationTransition transition = new ReservationTransition(
                reservationId, previousStatus, newStatus, userId, clock.instant(), reason);
        persistTransition(transition);
        return transition;
    }

    @Transactional
    public void recordTransition(ReservationTransition transition) {
        ReservationStatus currentStatus = lockCurrentStatus(transition.reservationId());
        if (currentStatus != transition.from()
                || !isAllowedTransition(currentStatus, transition.to())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation state has changed");
        }
        persistTransition(transition);
    }

    @Transactional(readOnly = true)
    public List<ReservationTransition> history(String reservationId) {
        return jdbcTemplate.query("""
                SELECT reservation_id, previous_status, new_status, user_id, occurred_at, reason
                FROM reservation_audit
                WHERE reservation_id = ?
                ORDER BY audit_id
                """,
                (resultSet, rowNumber) -> mapTransition(resultSet),
                reservationId);
    }

    @Transactional
    public void deleteReservation(String reservationId) {
        deleteReservation(reservationId, null, true);
    }

    @Transactional
    public void deleteReservation(String reservationId, String requesterId, boolean canDeleteAny) {
        ReservationLifecycle reservation = lockCurrentReservation(reservationId);
        if (requesterId != null && !canDeleteAny && !reservation.requesterId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to cancel this reservation");
        }
        ReservationStatus status = reservation.status();
        if (status == ReservationStatus.EM_USO || status == ReservationStatus.CONCLUIDA) {
            throw new ReservationDeletionConflictException();
        }
        int updated = jdbcTemplate.update(
                "UPDATE reservation_lifecycle SET deleted = TRUE WHERE reservation_id = ? AND deleted = FALSE",
                reservationId);
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found");
        }
    }

    private ReservationLifecycle lockCurrentReservation(String reservationId) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT requester_id, status
                    FROM reservation_lifecycle
                    WHERE reservation_id = ? AND deleted = FALSE
                    FOR UPDATE
                    """,
                    (resultSet, rowNumber) -> new ReservationLifecycle(
                            resultSet.getString("requester_id"),
                            ReservationStatus.valueOf(resultSet.getString("status"))),
                    reservationId);
        } catch (org.springframework.dao.EmptyResultDataAccessException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found", exception);
        }
    }

    private ReservationStatus lockCurrentStatus(String reservationId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT status FROM reservation_lifecycle WHERE reservation_id = ? AND deleted = FALSE FOR UPDATE",
                    (resultSet, rowNumber) -> ReservationStatus.valueOf(resultSet.getString(1)),
                    reservationId);
        } catch (org.springframework.dao.EmptyResultDataAccessException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found", exception);
        }
    }

    private void persistTransition(ReservationTransition transition) {
        int updated = jdbcTemplate.update("""
                UPDATE reservation_lifecycle
                SET status = ?
                WHERE reservation_id = ? AND status = ? AND deleted = FALSE
                """,
                transition.to().name(),
                transition.reservationId(),
                transition.from().name());
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation state has changed");
        }
        insertAudit(transition);
    }

    private void insertAudit(ReservationTransition transition) {
        jdbcTemplate.update("""
                INSERT INTO reservation_audit
                    (reservation_id, previous_status, new_status, user_id, occurred_at, reason)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                transition.reservationId(),
                transition.from() == null ? null : transition.from().name(),
                transition.to().name(),
                transition.actorId(),
                java.sql.Timestamp.from(transition.changedAt()),
                transition.reason());
    }

    private ReservationTransition mapTransition(ResultSet resultSet) throws SQLException {
        String previousStatus = resultSet.getString("previous_status");
        return new ReservationTransition(
                resultSet.getString("reservation_id"),
                previousStatus == null ? null : ReservationStatus.valueOf(previousStatus),
                ReservationStatus.valueOf(resultSet.getString("new_status")),
                resultSet.getString("user_id"),
                resultSet.getTimestamp("occurred_at").toInstant(),
                resultSet.getString("reason"));
    }

    private boolean isAllowedTransition(ReservationStatus previousStatus, ReservationStatus newStatus) {
        return switch (previousStatus) {
            case SOLICITADA -> newStatus == ReservationStatus.APROVADA;
            case APROVADA -> newStatus == ReservationStatus.EM_USO;
            case EM_USO -> newStatus == ReservationStatus.CONCLUIDA;
            case CONCLUIDA -> false;
        };
    }

    private record ReservationLifecycle(String requesterId, ReservationStatus status) {
    }
}
