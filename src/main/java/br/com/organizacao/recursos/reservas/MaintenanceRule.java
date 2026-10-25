package br.com.organizacao.recursos.reservas;

import java.time.Clock;
import java.time.Instant;

public final class MaintenanceRule {
    private final Clock clock;

    public MaintenanceRule(Clock clock) {
        this.clock = clock;
    }

    public boolean isActiveNow(MaintenancePeriod maintenance) {
        Instant now = clock.instant();
        return !now.isBefore(maintenance.startsAt()) && now.isBefore(maintenance.endsAt());
    }

    public boolean isReservable(ReservationPeriod reservation, MaintenancePeriod maintenance) {
        boolean overlaps = reservation.startsAt().isBefore(maintenance.endsAt())
                && reservation.endsAt().isAfter(maintenance.startsAt());
        return !overlaps;
    }
}
