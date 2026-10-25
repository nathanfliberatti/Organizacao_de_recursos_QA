package br.com.organizacao.recursos.reservas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ReservationApprovalTest {

    @Test
    void restrictedResourceReservationRemainsRequestedUntilResponsibleApproval() {
        Reservation reservation = Reservation.requestForRestrictedResource();

        assertEquals(ReservationStatus.SOLICITADA, reservation.status());
    }
}
