package br.com.organizacao.recursos.reservas;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReservationLifecycleTest {

    @Test
    void everyStateChangeUsesClockAndRecordsReasonAndReservationId() {
        Reservation reservation = Reservation.requestForRestrictedResource(
                "reservation-42", "requester-1");
        User responsible = new User("responsible-1", UserRole.RESPONSAVEL);
        Instant changedAt = Instant.parse("2026-10-25T12:00:00Z");
        Clock clock = Clock.fixed(changedAt, ZoneOffset.UTC);

        reservation.approve(responsible, clock, "Aprovada pelo responsável");
        reservation.startUse(responsible, clock, "Uso iniciado");
        reservation.complete(responsible, clock, "Uso concluído");

        assertEquals(List.of(
                new ReservationTransition("reservation-42", ReservationStatus.SOLICITADA,
                        ReservationStatus.APROVADA, responsible.id(), changedAt,
                        "Aprovada pelo responsável"),
                new ReservationTransition("reservation-42", ReservationStatus.APROVADA,
                        ReservationStatus.EM_USO, responsible.id(), changedAt, "Uso iniciado"),
                new ReservationTransition("reservation-42", ReservationStatus.EM_USO,
                        ReservationStatus.CONCLUIDA, responsible.id(), changedAt, "Uso concluído")),
                reservation.transitions());
    }

    @Test
    void reservationCannotBeDeletedAfterItHasStarted() {
        Reservation reservation = Reservation.requestForRestrictedResource(
                "reservation-42", "requester-1");
        User responsible = new User("responsible-1", UserRole.RESPONSAVEL);
        Clock clock = Clock.fixed(Instant.parse("2026-10-25T12:00:00Z"), ZoneOffset.UTC);

        assertEquals(true, reservation.canBeDeleted());

        reservation.approve(responsible, clock, "Aprovada pelo responsável");
        reservation.startUse(responsible, clock, "Uso iniciado");

        assertEquals(false, reservation.canBeDeleted());
    }

    @Test
    void rejectsTransitionThatDoesNotMatchReservationIdOrCurrentState() {
        Reservation reservation = Reservation.requestForRestrictedResource(
                "reservation-42", "requester-1");
        Instant changedAt = Instant.parse("2026-10-25T12:00:00Z");
        ReservationTransition wrongId = new ReservationTransition(
                "another-reservation", ReservationStatus.SOLICITADA, ReservationStatus.APROVADA,
                "responsible-1", changedAt, "Aprovada");
        ReservationTransition wrongState = new ReservationTransition(
                "reservation-42", ReservationStatus.APROVADA, ReservationStatus.EM_USO,
                "responsible-1", changedAt, "Iniciada");

        assertThrows(IllegalStateException.class, () -> reservation.applyTransition(wrongId));
        assertThrows(IllegalStateException.class, () -> reservation.applyTransition(wrongState));
        assertEquals(ReservationStatus.SOLICITADA, reservation.status());
        assertEquals(true, reservation.canBeDeleted());
    }

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
                        new ReservationTransition(reservation.id(), ReservationStatus.SOLICITADA,
                                ReservationStatus.APROVADA, responsible.id(), changedAt,
                                "Reserva aprovada pelo Responsável"),
                        new ReservationTransition(reservation.id(), ReservationStatus.APROVADA,
                                ReservationStatus.EM_USO, responsible.id(), changedAt, "Reserva iniciada"),
                        new ReservationTransition(reservation.id(), ReservationStatus.EM_USO,
                                ReservationStatus.CONCLUIDA, responsible.id(), changedAt,
                                "Reserva concluída")),
                        reservation.transitions()));
    }

    @Test
    void completedReservationPreservesApprovalDecisionWhenApprovalIsRepeated() {
        Reservation reservation = Reservation.requestForRestrictedResource("requester-1");
        User responsible = new User("responsible-1", UserRole.RESPONSAVEL);
        Clock originalClock = Clock.fixed(
                Instant.parse("2026-10-25T12:00:00Z"), ZoneOffset.UTC);

        reservation.approve(responsible, originalClock);
        reservation.startUse(responsible, originalClock);
        reservation.complete(responsible, originalClock);
        ApprovalDecision originalDecision = reservation.approvalDecision();
        List<ReservationTransition> originalTransitions = reservation.transitions();

        reservation.approve(responsible, Clock.fixed(
                Instant.parse("2026-10-25T13:00:00Z"), ZoneOffset.UTC));

        assertAll(
                () -> assertEquals(ReservationStatus.CONCLUIDA, reservation.status()),
                () -> assertEquals(originalDecision, reservation.approvalDecision()),
                () -> assertEquals(originalTransitions, reservation.transitions()));
    }

        @Test
        void reservationCannotStartUseBeforeApproval() {
                Reservation reservation = Reservation.requestForRestrictedResource("requester-1");
                User responsible = new User("responsible-1", UserRole.RESPONSAVEL);
                Clock clock = Clock.fixed(Instant.parse("2026-10-25T12:00:00Z"), ZoneOffset.UTC);

                reservation.startUse(responsible, clock);

                assertAll(
                                () -> assertEquals(ReservationStatus.SOLICITADA, reservation.status()),
                                () -> assertEquals(List.of(), reservation.transitions()));
        }
}
