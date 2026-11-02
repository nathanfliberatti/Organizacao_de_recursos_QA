package br.com.organizacao.recursos.reservas;

import java.time.Instant;
import java.util.Objects;

public record ReservationTransition(
        String reservationId,
        ReservationStatus from,
        ReservationStatus to,
        String actorId,
        Instant changedAt,
        String reason) {
    public ReservationTransition {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("Reservation ID is required");
        }
        Objects.requireNonNull(to, "New reservation status is required");
        if (from == null && to != ReservationStatus.SOLICITADA) {
            throw new IllegalArgumentException("Only a new request can have no previous status");
        }
        Objects.requireNonNull(actorId, "Actor ID is required");
        if (actorId.isBlank()) {
            throw new IllegalArgumentException("Actor ID is required");
        }
        Objects.requireNonNull(changedAt, "Change timestamp is required");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Audit reason is required");
        }
    }
}
