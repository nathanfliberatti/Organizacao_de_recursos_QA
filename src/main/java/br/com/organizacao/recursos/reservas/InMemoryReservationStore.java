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
import org.springframework.stereotype.Component;

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
    public OptionalLong scheduleWithId(
            String roomId, String professorId, String requesterId, ReservationPeriod period) {
        long id = reservationIds.incrementAndGet();
        reservations.put(id, new ScheduledReservation(roomId, professorId, requesterId, period, false));
        return OptionalLong.of(id);
    }

    @Override
    public void recordConflictAttempt(String roomId, String professorId, ReservationPeriod period) {
        rejectedAttempts.add(new RejectedAttempt(roomId, professorId, period));
    }

    @Override
    public boolean isAvailable(String roomId, String professorId, ReservationPeriod period) {
        return true;
    }

    @Override
    public Optional<String> requesterId(long reservationId) {
        ScheduledReservation reservation = reservations.get(reservationId);
        return reservation == null ? Optional.empty() : Optional.of(reservation.requesterId());
    }

    @Override
    public void update(long reservationId, ReservationPeriod period) {
        reservations.compute(reservationId, (id, reservation) -> {
            if (reservation == null) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Reservation not found");
            }
            return new ScheduledReservation(
                    reservation.roomId(), reservation.professorId(), reservation.requesterId(), period,
                    reservation.cancelled());
        });
    }

    @Override
    public List<ReservationSummary> list(String requesterId) {
        return reservations.entrySet().stream()
                .filter(entry -> requesterId == null || requesterId.equals(entry.getValue().requesterId()))
                .sorted(Comparator.comparing((java.util.Map.Entry<Long, ScheduledReservation> entry) ->
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
    public boolean cancel(long reservationId) {
        boolean[] cancelled = {false};
        reservations.computeIfPresent(reservationId, (id, reservation) -> {
            if (reservation.cancelled()) {
                return reservation;
            }
            cancelled[0] = true;
            return new ScheduledReservation(reservation.roomId(), reservation.professorId(),
                    reservation.requesterId(), reservation.period(), true);
        });
        return cancelled[0];
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
