package br.com.organizacao.recursos.reservas;

import java.time.Clock;
public final class MaintenanceRule {
    private final Clock clock;

    public MaintenanceRule(Clock clock) {
        this.clock = clock;
    }

    public boolean isActiveNow(MaintenancePeriod maintenance) {
        return maintenance.contains(clock.instant());
    }

    public boolean isReservable(ReservationPeriod reservation, MaintenancePeriod maintenance) {
        return !maintenance.overlaps(reservation);
    }
}
