package br.com.organizacao.recursos.reservas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** RF-14: estados alternativos REJEITADA, CANCELADA e NAO_COMPARECEU e quem pode produzi-los. */
class ReservationAlternativeStatesTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneOffset.UTC);
    private static final User OWNER = new User("requester-1", UserRole.SOLICITANTE);
    private static final User RESPONSIBLE = new User("responsible-1", UserRole.RESPONSAVEL);
    private static final User ADMIN = new User("admin-1", UserRole.ADMINISTRADOR);

    private static Reservation requested() {
        return Reservation.requestForRestrictedResource("reservation-1", OWNER.id());
    }

    private static Reservation approved() {
        Reservation reservation = requested();
        reservation.approve(RESPONSIBLE, CLOCK);
        return reservation;
    }

    private static void apply(Reservation reservation, ReservationTransition transition) {
        assertNotNull(transition);
        reservation.applyTransition(transition);
    }

    @Test
    void responsibleRejectsARequestAndItBecomesAFinalState() {
        Reservation reservation = requested();

        apply(reservation, reservation.prepareRejection(RESPONSIBLE, CLOCK, "Sem justificativa"));

        assertEquals(ReservationStatus.REJEITADA, reservation.status());
        assertNull(reservation.prepareApproval(RESPONSIBLE, CLOCK, "tarde demais"));
        assertNull(reservation.prepareCancellation(OWNER, CLOCK, "tarde demais"));
        assertEquals(ReservationStatus.REJEITADA, reservation.transitions().getLast().to());
    }

    @Test
    void requesterAndOtherProfilesCannotReject() {
        Reservation reservation = requested();

        assertNull(reservation.prepareRejection(OWNER, CLOCK, "x"));
        assertNull(reservation.prepareRejection(ADMIN, CLOCK, "x"));
        assertNull(reservation.prepareRejection(
                new User(OWNER.id(), UserRole.RESPONSAVEL), CLOCK, "o responsável não rejeita a própria solicitação"));
        assertEquals(ReservationStatus.SOLICITADA, reservation.status());
    }

    @Test
    void approvedReservationCannotBeRejected() {
        assertNull(approved().prepareRejection(RESPONSIBLE, CLOCK, "x"));
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"SOLICITADA", "APROVADA"})
    void reservationCanBeCancelledBeforeItStarts(ReservationStatus startingPoint) {
        Reservation reservation = startingPoint == ReservationStatus.SOLICITADA ? requested() : approved();

        apply(reservation, reservation.prepareCancellation(OWNER, CLOCK, "Mudança de planos"));

        assertEquals(ReservationStatus.CANCELADA, reservation.status());
        assertNull(reservation.prepareStartUse(OWNER, CLOCK, "x"));
    }

    @Test
    void startedReservationCannotBeCancelled() {
        Reservation reservation = approved();
        reservation.startUse(OWNER, CLOCK);

        assertNull(reservation.prepareCancellation(OWNER, CLOCK, "x"));
        assertEquals(ReservationStatus.EM_USO, reservation.status());
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"RESPONSAVEL", "ADMINISTRADOR"})
    void responsibleAndAdministratorRegisterNoShowOfAnApprovedReservation(UserRole role) {
        Reservation reservation = approved();

        apply(reservation, reservation.prepareNoShow(new User("manager-1", role), CLOCK, "Não compareceu"));

        assertEquals(ReservationStatus.NAO_COMPARECEU, reservation.status());
        assertNull(reservation.prepareStartUse(OWNER, CLOCK, "x"));
    }

    @Test
    void requesterCannotRegisterNoShowAndRequestedReservationHasNoNoShow() {
        assertNull(approved().prepareNoShow(OWNER, CLOCK, "x"));
        assertNull(requested().prepareNoShow(RESPONSIBLE, CLOCK, "ainda não foi aprovada"));
    }

    @Test
    void transitionsOutOfASkippedStateAreRefusedByApply() {
        Reservation reservation = requested();
        ReservationTransition noShow = new ReservationTransition("reservation-1", ReservationStatus.SOLICITADA,
                ReservationStatus.NAO_COMPARECEU, RESPONSIBLE.id(), CLOCK.instant(), "salto inválido");

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> reservation.applyTransition(noShow));
    }
}
