package br.com.organizacao.recursos.reservas;

public interface ReservationStore {
    boolean schedule(String roomId, String professorId, String requesterId, ReservationPeriod period);

    void recordConflictAttempt(String roomId, String professorId, ReservationPeriod period);

    boolean isAvailable(String roomId, String professorId, ReservationPeriod period);
}
