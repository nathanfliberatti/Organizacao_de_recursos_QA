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
    private final Map<String, String> requesterIds = new ConcurrentHashMap<>();
    private final Map<String, List<ReservationTransition>> audit = new ConcurrentHashMap<>();
    private final Map<String, Boolean> deleted = new ConcurrentHashMap<>();

    public InMemoryReservationAuditService() {
        super(null);
    }

    public synchronized void clear() {
        statuses.clear();
        requesterIds.clear();
        audit.clear();
        deleted.clear();
    }

    @Override
    public synchronized void createReservation(
            String reservationId, String requesterId, Clock clock, String reason) {
        if (statuses.putIfAbsent(reservationId, ReservationStatus.SOLICITADA) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation already exists");
        }
        requesterIds.put(reservationId, requesterId);
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
        if (!isAllowedInMemoryTransition(previousStatus, newStatus)) {
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
                || !isAllowedInMemoryTransition(currentStatus, transition.to())) {
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
    public synchronized void assertCanOperate(
            String reservationId, String requesterId, boolean canOperateAny) {
        currentStatus(reservationId);
        if (!canOperateAny && !requesterIds.get(reservationId).equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to operate this reservation");
        }
    }

    @Override
    public synchronized void deleteReservation(String reservationId) {
        deleteReservation(reservationId, null, true);
    }

    @Override
    public synchronized void deleteReservation(
            String reservationId, String requesterId, boolean canDeleteAny) {
        ReservationStatus status = currentStatus(reservationId);
        if (requesterId != null && !canDeleteAny && !requesterIds.get(reservationId).equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to cancel this reservation");
        }
        if (status == ReservationStatus.EM_USO || status == ReservationStatus.CONCLUIDA) {
            throw new ReservationDeletionConflictException();
        }
        if (Boolean.TRUE.equals(deleted.get(reservationId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found");
        }
        deleted.put(reservationId, true);
    }

    @Override
    public synchronized ReservationTransition cancelReservation(
            String reservationId, String actorId, boolean canCancelAny, Clock clock, String reason) {
        ReservationStatus status = currentStatus(reservationId);
        if (!canCancelAny && !requesterIds.get(reservationId).equals(actorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to cancel this reservation");
        }
        if (status.started()) {
            throw new ReservationDeletionConflictException();
        }
        if (!status.canMoveTo(ReservationStatus.CANCELADA)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation can no longer be cancelled");
        }
        ReservationTransition transition = new ReservationTransition(
                reservationId, status, ReservationStatus.CANCELADA, actorId, clock.instant(), reason);
        recordTransition(transition);
        return transition;
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

    private boolean isAllowedInMemoryTransition(ReservationStatus previousStatus, ReservationStatus newStatus) {
        return previousStatus.canMoveTo(newStatus);
    }
}
