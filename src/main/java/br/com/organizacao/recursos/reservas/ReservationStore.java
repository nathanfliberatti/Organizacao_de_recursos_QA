package br.com.organizacao.recursos.reservas;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.List;

public interface ReservationStore {
    boolean schedule(String roomId, String professorId, String requesterId, ReservationPeriod period);

    OptionalLong scheduleWithId(
            String roomId, String professorId, String requesterId, ReservationPeriod period);

    void recordConflictAttempt(String roomId, String professorId, ReservationPeriod period);

    boolean isAvailable(String roomId, String professorId, ReservationPeriod period);

    Optional<String> requesterId(long reservationId);

    List<ScheduledReservation> findByRequesterId(String requesterId);

    void update(long reservationId, ReservationPeriod period);
}
