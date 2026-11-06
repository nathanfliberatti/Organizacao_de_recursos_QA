package br.com.organizacao.recursos.reservas;

import java.time.Instant;

public record ScheduledReservation(
        long id,
        String resourceId,
        String professorId,
        String requesterId,
        Instant startsAt,
        Instant endsAt) {
}