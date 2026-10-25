package br.com.organizacao.recursos.reservas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ReservationApprovalTest {

    @Test
    void restrictedResourceReservationRemainsRequestedUntilResponsibleApproval() {
        Reservation reservation = restrictedReservation();

        assertEquals(ReservationStatus.SOLICITADA, reservation.status());
    }

    @Test
    void responsibleApprovalRecordsDecisionAndMovesReservationToApproved() {
        Reservation reservation = restrictedReservation();
        User responsible = new User("responsible-1", UserRole.RESPONSAVEL);
        Instant decisionTime = Instant.parse("2026-10-25T12:00:00Z");

        reservation.approve(responsible, Clock.fixed(decisionTime, ZoneOffset.UTC));

        assertAll(
                () -> assertEquals(ReservationStatus.APROVADA, reservation.status()),
                () -> assertEquals(new ApprovalDecision(responsible, decisionTime),
                        reservation.approvalDecision()));
    }

    @Test
    void requesterCannotApproveOwnReservationEvenWithResponsibleRole() {
        User requesterAndApprover = new User("same-user", UserRole.RESPONSAVEL);
        Reservation reservation = restrictedReservation(requesterAndApprover.id());

        reservation.approve(requesterAndApprover, Clock.fixed(
                Instant.parse("2026-10-25T12:00:00Z"), ZoneOffset.UTC));

        assertAll(
                () -> assertEquals(ReservationStatus.SOLICITADA, reservation.status()),
                () -> assertNull(reservation.approvalDecision()));
    }

    @Test
    void requesterCannotApproveAnotherUsersRestrictedReservation() {
        Reservation reservation = restrictedReservation("requester-1");
        User requester = new User("requester-2", UserRole.SOLICITANTE);
        Clock clock = Clock.fixed(Instant.parse("2026-10-25T12:00:00Z"), ZoneOffset.UTC);

        reservation.approve(requester, clock);

        assertAll(
                () -> assertEquals(ReservationStatus.SOLICITADA, reservation.status()),
                () -> assertNull(reservation.approvalDecision()));
    }

    private Reservation restrictedReservation() {
        return restrictedReservation("requester-1");
    }

    private Reservation restrictedReservation(String requesterId) {
        return Reservation.requestForRestrictedResource(requesterId);
    }
}
