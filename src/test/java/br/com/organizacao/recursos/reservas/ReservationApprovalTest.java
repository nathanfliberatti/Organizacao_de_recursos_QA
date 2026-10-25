package br.com.organizacao.recursos.reservas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;

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

    private Reservation restrictedReservation() {
        return Reservation.requestForRestrictedResource();
    }
}
