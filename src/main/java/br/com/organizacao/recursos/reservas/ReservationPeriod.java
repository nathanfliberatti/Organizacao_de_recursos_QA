package br.com.organizacao.recursos.reservas;

import java.time.Instant;

public record ReservationPeriod(Instant startsAt, Instant endsAt) {
}
