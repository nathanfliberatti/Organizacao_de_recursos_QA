package br.com.organizacao.recursos.reservas;

import java.time.Instant;

public record ReservationTransition(
        ReservationStatus from,
        ReservationStatus to,
        String actorId,
        Instant changedAt) {
}
