package br.com.organizacao.recursos.reservas;

import java.time.Instant;

public record MaintenancePeriod(Instant startsAt, Instant endsAt) {
}
