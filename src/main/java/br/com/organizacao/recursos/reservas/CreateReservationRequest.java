package br.com.organizacao.recursos.reservas;

import java.time.Instant;

public record CreateReservationRequest(String resourceId, Instant startsAt, Instant endsAt) {
}
