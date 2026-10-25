package br.com.organizacao.recursos.reservas;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReservationLifecycleTest {

    @Test
    void reservationFollowsMainLifecycleAndAuditsEveryTransition() {
        Reservation reservation = Reservation.requestForRestrictedResource("requester-1");
        User responsible = new User("responsible-1", UserRole.RESPONSAVEL);
        Instant changedAt = Instant.parse("2026-10-25T12:00:00Z");
        Clock clock = Clock.fixed(changedAt, ZoneOffset.UTC);

        reservation.approve(responsible, clock);
        reservation.startUse(responsible, clock);
        reservation.complete(responsible, clock);

        assertAll(
                () -> assertEquals(ReservationStatus.CONCLUIDA, reservation.status()),
                () -> assertEquals(List.of(
                        new ReservationTransition(ReservationStatus.SOLICITADA,
                                ReservationStatus.APROVADA, responsible.id(), changedAt),
                        new ReservationTransition(ReservationStatus.APROVADA,
                                ReservationStatus.EM_USO, responsible.id(), changedAt),
                        new ReservationTransition(ReservationStatus.EM_USO,
                                ReservationStatus.CONCLUIDA, responsible.id(), changedAt)),
                        reservation.transitions()));
    }
}
