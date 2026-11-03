package br.com.organizacao.recursos.reservas;

public record ReservationAttemptResponse(boolean accepted, Long reservationId) {
    public ReservationAttemptResponse(boolean accepted) {
        this(accepted, null);
    }
}
