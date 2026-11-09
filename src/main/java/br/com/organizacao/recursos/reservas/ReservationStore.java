package br.com.organizacao.recursos.reservas;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

public interface ReservationStore {
    boolean schedule(String roomId, String professorId, String requesterId, ReservationPeriod period);

    OptionalLong scheduleWithId(
            String roomId, String professorId, String requesterId, ReservationPeriod period);

    void recordConflictAttempt(String roomId, String professorId, ReservationPeriod period);

    boolean isAvailable(String roomId, String professorId, ReservationPeriod period);

    Optional<String> requesterId(long reservationId);

    void update(long reservationId, ReservationPeriod period);

    /** Lista reservas; {@code requesterId} nulo retorna as de todos os solicitantes. */
    List<ReservationSummary> list(String requesterId);

    Optional<ReservationSummary> find(long reservationId);

    /** Marca a reserva como CANCELADA, liberando sala e professor. Retorna false se nao havia reserva ativa. */
    boolean cancel(long reservationId);
}
