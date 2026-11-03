package br.com.organizacao.recursos.reservas;

import java.time.Instant;

public record UpdateReservationRequest(Instant startsAt, Instant endsAt) {
}
