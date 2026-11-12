package br.com.organizacao.recursos.reservas;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Armazenamento em memória (somente desenvolvimento e demonstração). Aplica a mesma regra de integridade
 * do PostgreSQL: reservas ativas da mesma sala ou do mesmo professor não podem se sobrepor, usando
 * intervalos semiabertos {@code [início, fim)}. A verificação e a gravação são atômicas (bloco sincronizado),
 * então duas solicitações simultâneas nunca confirmam reservas incompatíveis.
 */
@Component
@Profile("!postgres")
public class InMemoryReservationStore implements ReservationStore {
    private final List<RejectedAttempt> rejectedAttempts = new CopyOnWriteArrayList<>();
    private final Map<Long, ScheduledReservation> reservations = new ConcurrentHashMap<>();
    private final AtomicLong reservationIds = new AtomicLong();

    @Override
    public boolean schedule(String roomId, String professorId, String requesterId, ReservationPeriod period) {
        return scheduleWithId(roomId, professorId, requesterId, period).isPresent();
    }

    @Override
    public synchronized OptionalLong scheduleWithId(
            String roomId, String professorId, String requesterId, ReservationPeriod period) {
        if (!isAvailable(roomId, professorId, period)) {
            return OptionalLong.empty();
        }
        long id = reservationIds.incrementAndGet();
        reservations.put(id, new ScheduledReservation(roomId, professorId, requesterId, period, false));
        return OptionalLong.of(id);
    }

    @Override
    public void recordConflictAttempt(String roomId, String professorId, ReservationPeriod period) {
        rejectedAttempts.add(new RejectedAttempt(roomId, professorId, period));
    }

    @Override
    public synchronized boolean isAvailable(String roomId, String professorId, ReservationPeriod period) {
        return conflictsWith(roomId, professorId, period, null).isEmpty();
    }

    @Override
    public Optional<String> requesterId(long reservationId) {
        ScheduledReservation reservation = reservations.get(reservationId);
        return reservation == null || reservation.cancelled()
                ? Optional.empty() : Optional.of(reservation.requesterId());
    }

    @Override
    public synchronized void update(long reservationId, ReservationPeriod period) {
        ScheduledReservation reservation = reservations.get(reservationId);
        if (reservation == null || reservation.cancelled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found");
        }
        if (conflictsWith(reservation.roomId(), reservation.professorId(), period, reservationId).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Reservation period overlaps another reservation");
        }
        reservations.put(reservationId, new ScheduledReservation(
                reservation.roomId(), reservation.professorId(), reservation.requesterId(), period, false));
    }

    @Override
    public List<ReservationSummary> list(String requesterId) {
        return reservations.entrySet().stream()
                .filter(entry -> requesterId == null || requesterId.equals(entry.getValue().requesterId()))
                .sorted(Comparator.comparing((Map.Entry<Long, ScheduledReservation> entry) ->
                        entry.getValue().period().startsAt()).reversed())
                .map(entry -> summary(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    public Optional<ReservationSummary> find(long reservationId) {
        ScheduledReservation reservation = reservations.get(reservationId);
        return reservation == null ? Optional.empty() : Optional.of(summary(reservationId, reservation));
    }

    @Override
    public synchronized boolean cancel(long reservationId) {
        ScheduledReservation reservation = reservations.get(reservationId);
        if (reservation == null || reservation.cancelled()) {
            return false;
        }
        reservations.put(reservationId, new ScheduledReservation(reservation.roomId(),
                reservation.professorId(), reservation.requesterId(), reservation.period(), true));
        return true;
    }

    /** Primeira reserva ativa (diferente de {@code ignoredId}) que usa a mesma sala ou o mesmo professor no período. */
    private Optional<Long> conflictsWith(
            String roomId, String professorId, ReservationPeriod period, Long ignoredId) {
        return reservations.entrySet().stream()
                .filter(entry -> !entry.getValue().cancelled())
                .filter(entry -> ignoredId == null || !entry.getKey().equals(ignoredId))
                .filter(entry -> overlaps(entry.getValue().period(), period))
                .filter(entry -> entry.getValue().roomId().equals(roomId)
                        || (professorId != null && professorId.equals(entry.getValue().professorId())))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    private static boolean overlaps(ReservationPeriod first, ReservationPeriod second) {
        return first.startsAt().isBefore(second.endsAt()) && second.startsAt().isBefore(first.endsAt());
    }

    private ReservationSummary summary(long id, ScheduledReservation reservation) {
        return new ReservationSummary(String.valueOf(id), reservation.roomId(), reservation.professorId(),
                reservation.requesterId(), reservation.period().startsAt(), reservation.period().endsAt(),
                reservation.cancelled() ? "CANCELADA" : "AGENDADA", null);
    }

    private record RejectedAttempt(String roomId, String professorId, ReservationPeriod period) {
    }

    private record ScheduledReservation(
            String roomId, String professorId, String requesterId, ReservationPeriod period, boolean cancelled) {
    }
}
