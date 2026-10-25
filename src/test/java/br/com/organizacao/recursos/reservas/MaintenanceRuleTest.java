package br.com.organizacao.recursos.reservas;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class MaintenanceRuleTest {

    @Test
    void reservationCrossingMaintenanceStartByOneSecondIsRejected() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-25T10:30:00Z"), ZoneOffset.UTC);
        MaintenanceRule rule = new MaintenanceRule(clock);
        MaintenancePeriod maintenance = new MaintenancePeriod(
                Instant.parse("2026-10-25T10:00:00Z"),
                Instant.parse("2026-10-25T11:00:00Z"));
        ReservationPeriod reservation = new ReservationPeriod(
                Instant.parse("2026-10-25T09:59:59Z"),
                Instant.parse("2026-10-25T10:00:01Z"));

        assertAll(
                () -> assertTrue(rule.isActiveNow(maintenance)),
                () -> assertFalse(rule.isReservable(reservation, maintenance)));
    }

        @Test
        void reservationOutsideMaintenancePeriodRemainsReservable() {
                Clock clock = Clock.fixed(Instant.parse("2026-10-25T12:30:00Z"), ZoneOffset.UTC);
                MaintenanceRule rule = new MaintenanceRule(clock);
                MaintenancePeriod maintenance = new MaintenancePeriod(
                                Instant.parse("2026-10-25T10:00:00Z"),
                                Instant.parse("2026-10-25T11:00:00Z"));
                ReservationPeriod reservation = new ReservationPeriod(
                                Instant.parse("2026-10-25T12:00:00Z"),
                                Instant.parse("2026-10-25T13:00:00Z"));

                assertTrue(rule.isReservable(reservation, maintenance));
        }
}
