package br.com.organizacao.recursos.reservas;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@Profile("!postgres")
public class InMemoryReservationAuditService extends ReservationAuditService {
    private final Map<String, ReservationStatus> statuses = new ConcurrentHashMap<>();
    private final Map<String, List<ReservationTransition>> audit = new ConcurrentHashMap<>();
    private final Map<String, Boolean> deleted = new ConcurrentHashMap<>();

    public InMemoryReservationAuditService() {
        super(null);
    }

    public synchronized void clear() {
        statuses.clear();
        audit.clear();
        deleted.clear();
    }

    @Override
    public synchronized void createReservation(
            String reservationId, String requesterId, Clock clock, String reason) {
        if (statuses.putIfAbsent(reservationId, ReservationStatus.SOLICITADA) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation already exists");
        }
        deleted.put(reservationId, false);
        audit.put(reservationId, new ArrayList<>(List.of(new ReservationTransition(
                reservationId,
                null,
                ReservationStatus.SOLICITADA,
                requesterId,
                clock.instant(),
                reason))));
    }

    @Override
    public synchronized ReservationTransition changeState(
            String reservationId,
            ReservationStatus newStatus,
            String userId,
            Clock clock,
            String reason) {
        ReservationStatus previousStatus = currentStatus(reservationId);
        if (!isAllowedTransition(previousStatus, newStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid reservation state transition");
        }
        ReservationTransition transition = new ReservationTransition(
                reservationId, previousStatus, newStatus, userId, clock.instant(), reason);
        recordTransition(transition);
        return transition;
    }

    @Override
    public synchronized void recordTransition(ReservationTransition transition) {
        ReservationStatus currentStatus = currentStatus(transition.reservationId());
        if (currentStatus != transition.from()
                || !isAllowedTransition(currentStatus, transition.to())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation state has changed");
        }
        statuses.put(transition.reservationId(), transition.to());
        audit.get(transition.reservationId()).add(transition);
    }

    @Override
    public synchronized List<ReservationTransition> history(String reservationId) {
        List<ReservationTransition> entries = audit.get(reservationId);
        return entries == null ? List.of() : List.copyOf(entries);
    }

    @Override
    public synchronized void deleteReservation(String reservationId) {
        ReservationStatus status = currentStatus(reservationId);
        if (status == ReservationStatus.EM_USO || status == ReservationStatus.CONCLUIDA) {
            throw new ReservationDeletionConflictException();
        }
        if (Boolean.TRUE.equals(deleted.get(reservationId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found");
        }
        deleted.put(reservationId, true);
    }

    private ReservationStatus currentStatus(String reservationId) {
        if (Boolean.TRUE.equals(deleted.get(reservationId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found");
        }
        ReservationStatus status = statuses.get(reservationId);
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found");
        }
        return status;
    }

    private boolean isAllowedTransition(ReservationStatus previousStatus, ReservationStatus newStatus) {
        return switch (previousStatus) {
            case SOLICITADA -> newStatus == ReservationStatus.APROVADA;
            case APROVADA -> newStatus == ReservationStatus.EM_USO;
            case EM_USO -> newStatus == ReservationStatus.CONCLUIDA;
            case CONCLUIDA -> false;
        };
    }
}
