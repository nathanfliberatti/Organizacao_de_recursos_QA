package br.com.organizacao.recursos.reservas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class InMemoryReservationAuditServiceTest {
    private final InMemoryReservationAuditService service = new InMemoryReservationAuditService();
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-02T22:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void clearAudit() {
        service.clear();
    }

    @Test
    void createsInitialAuditAndRejectsDuplicateReservation() {
        service.createReservation("reservation-1", "requester-1", clock, "Solicitação criada");

        ReservationTransition initial = service.history("reservation-1").getFirst();
        assertNull(initial.from());
        assertEquals(ReservationStatus.SOLICITADA, initial.to());
        assertEquals(clock.instant(), initial.changedAt());
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.createReservation("reservation-1", "requester-1", clock, "Duplicada"))
                .getStatusCode());
    }

    @Test
    void recordsOnlyAllowedTransitionsAndRejectsStaleOrInvalidTransitions() {
        service.createReservation("reservation-1", "requester-1", clock, "Solicitação criada");
        ReservationTransition stale = new ReservationTransition(
                "reservation-1", ReservationStatus.APROVADA, ReservationStatus.EM_USO,
                "responsible-1", clock.instant(), "Estado desatualizado");
        ReservationTransition invalid = new ReservationTransition(
                "reservation-1", ReservationStatus.SOLICITADA, ReservationStatus.EM_USO,
                "responsible-1", clock.instant(), "Transição inválida");

        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ResponseStatusException.class, () -> service.recordTransition(stale)).getStatusCode());
        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ResponseStatusException.class, () -> service.recordTransition(invalid)).getStatusCode());

        ReservationTransition approved = service.changeState(
                "reservation-1", ReservationStatus.APROVADA,
                "responsible-1", clock, "Aprovada");
        assertEquals(ReservationStatus.SOLICITADA, approved.from());
        assertEquals(ReservationStatus.APROVADA, approved.to());
        assertEquals(2, service.history("reservation-1").size());
    }

    @Test
    void enforcesReservationOwnershipAndAllowsAuthorizedOperators() {
        service.createReservation("reservation-1", "requester-1", clock, "Solicitação criada");

        service.assertCanOperate("reservation-1", "requester-1", false);
        service.assertCanOperate("reservation-1", "other-requester", true);
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ResponseStatusException.class,
                        () -> service.assertCanOperate("reservation-1", "other-requester", false))
                        .getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ResponseStatusException.class,
                        () -> service.assertCanOperate("missing", "requester-1", true))
                        .getStatusCode());
    }

    @Test
    void restrictsDeletionByOwnerAndReservationState() {
        service.createReservation("reservation-1", "requester-1", clock, "Solicitação criada");

        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ResponseStatusException.class,
                        () -> service.deleteReservation("reservation-1", "other-requester", false))
                        .getStatusCode());
        service.deleteReservation("reservation-1", "requester-1", false);
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ResponseStatusException.class,
                        () -> service.deleteReservation("reservation-1"))
                        .getStatusCode());

        service.createReservation("reservation-2", "requester-1", clock, "Solicitação criada");
        service.deleteReservation("reservation-2");

        service.createReservation("reservation-3", "requester-1", clock, "Solicitação criada");
        service.changeState("reservation-3", ReservationStatus.APROVADA,
                "responsible-1", clock, "Aprovada");
        service.changeState("reservation-3", ReservationStatus.EM_USO,
                "responsible-1", clock, "Uso iniciado");
        assertThrows(ReservationDeletionConflictException.class,
                () -> service.deleteReservation("reservation-3"));

        service.createReservation("reservation-4", "requester-1", clock, "Solicitação criada");
        service.changeState("reservation-4", ReservationStatus.APROVADA,
                "responsible-1", clock, "Aprovada");
        service.changeState("reservation-4", ReservationStatus.EM_USO,
                "responsible-1", clock, "Uso iniciado");
        service.changeState("reservation-4", ReservationStatus.CONCLUIDA,
                "responsible-1", clock, "Concluída");
        assertThrows(ReservationDeletionConflictException.class,
                () -> service.deleteReservation("reservation-4"));
    }

    @Test
    void cancellationIsAnAuditedTransitionRestrictedByOwnerAndState() {
        service.createReservation("reservation-c1", "requester-1", clock, "Solicitação criada");

        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> service.cancelReservation("reservation-c1", "other", false, clock, "x")).getStatusCode());
        ReservationTransition transition = service.cancelReservation(
                "reservation-c1", "requester-1", false, clock, "Mudança de planos");

        assertEquals(ReservationStatus.CANCELADA, transition.to());
        assertEquals(List.of(ReservationStatus.SOLICITADA, ReservationStatus.CANCELADA),
                service.history("reservation-c1").stream().map(ReservationTransition::to).toList());
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.cancelReservation("reservation-c1", "requester-1", false, clock, "de novo"))
                .getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> service.cancelReservation("inexistente", "requester-1", true, clock, "x")).getStatusCode());
    }

    @Test
    void managerCanCancelAnotherRequestersReservationButNotAStartedOne() {
        service.createReservation("reservation-c2", "requester-1", clock, "Solicitação criada");
        service.cancelReservation("reservation-c2", "manager-1", true, clock, "Cancelada pelo gestor");

        service.createReservation("reservation-c3", "requester-1", clock, "Solicitação criada");
        service.changeState("reservation-c3", ReservationStatus.APROVADA, "responsible-1", clock, "Aprovada");
        service.changeState("reservation-c3", ReservationStatus.EM_USO, "responsible-1", clock, "Iniciada");
        assertThrows(ReservationDeletionConflictException.class,
                () -> service.cancelReservation("reservation-c3", "manager-1", true, clock, "x"));
    }
}
