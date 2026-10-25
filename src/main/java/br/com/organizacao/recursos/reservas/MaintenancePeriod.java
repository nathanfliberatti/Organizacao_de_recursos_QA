package br.com.organizacao.recursos.reservas;

import java.time.Instant;

public record MaintenancePeriod(Instant startsAt, Instant endsAt) {
	public boolean contains(Instant instant) {
		return !instant.isBefore(startsAt) && instant.isBefore(endsAt);
	}

	public boolean overlaps(ReservationPeriod reservation) {
		return reservation.startsAt().isBefore(endsAt)
				&& reservation.endsAt().isAfter(startsAt);
	}
}
