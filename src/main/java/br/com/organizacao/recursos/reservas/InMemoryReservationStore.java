package br.com.organizacao.recursos.reservas;

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
    private final Map<Long, StoredReservation> reservations = new ConcurrentHashMap<>();
    private final AtomicLong reservationIds = new AtomicLong();

    @Override
    public boolean schedule(String roomId, String professorId, String requesterId, ReservationPeriod period) {
        return scheduleWithId(roomId, professorId, requesterId, period).isPresent();
    }

    @Override
    public OptionalLong scheduleWithId(
            String roomId, String professorId, String requesterId, ReservationPeriod period) {
        long id = reservationIds.incrementAndGet();
        reservations.put(id, new StoredReservation(roomId, professorId, requesterId, period));
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
        StoredReservation reservation = reservations.get(reservationId);
        return reservation == null ? Optional.empty() : Optional.of(reservation.requesterId());
    }

    @Override
    public List<ScheduledReservation> findByRequesterId(String requesterId) {
        return reservations.entrySet().stream()
                .filter(entry -> entry.getValue().requesterId().equals(requesterId))
                .map(entry -> new ScheduledReservation(
                        entry.getKey(),
                        entry.getValue().roomId(),
                        entry.getValue().professorId(),
                        entry.getValue().requesterId(),
                        entry.getValue().period().startsAt(),
                        entry.getValue().period().endsAt()))
                .sorted(java.util.Comparator.comparingLong(ScheduledReservation::id))
                .toList();
    }

    @Override
    public void update(long reservationId, ReservationPeriod period) {
        reservations.compute(reservationId, (id, reservation) -> {
            if (reservation == null) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Reservation not found");
            }
                return new StoredReservation(
                    reservation.roomId(), reservation.professorId(), reservation.requesterId(), period);
        });
    }

    private record RejectedAttempt(String roomId, String professorId, ReservationPeriod period) {
    }

    private record StoredReservation(
            String roomId, String professorId, String requesterId, ReservationPeriod period) {
    }
}
