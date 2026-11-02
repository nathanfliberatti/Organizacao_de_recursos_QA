package br.com.organizacao.recursos.reservas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ReservationAuditEntryTest {
    @Test
    void recordsReservationActorTimestampStatesAndReason() {
        Instant changedAt = Instant.parse("2026-11-02T22:00:00Z");
        ReservationTransition entry = new ReservationTransition(
                "reservation-42",
                ReservationStatus.SOLICITADA,
                ReservationStatus.APROVADA,
                "responsible-7",
                changedAt,
                "Aprovada pelo responsável");

        assertEquals("reservation-42", entry.reservationId());
        assertEquals(ReservationStatus.SOLICITADA, entry.from());
        assertEquals(ReservationStatus.APROVADA, entry.to());
        assertEquals("responsible-7", entry.actorId());
        assertEquals(changedAt, entry.changedAt());
        assertEquals("Aprovada pelo responsável", entry.reason());
    }

    @Test
    void rejectsBlankAuditReason() {
        assertThrows(IllegalArgumentException.class, () -> new ReservationTransition(
                "reservation-42",
                ReservationStatus.SOLICITADA,
                ReservationStatus.APROVADA,
                "responsible-7",
                Instant.parse("2026-11-02T22:00:00Z"),
                " "));
    }

    @Test
    void permitsMissingPreviousStateOnlyForInitialRequest() {
        ReservationTransition created = new ReservationTransition(
                "reservation-42",
                null,
                ReservationStatus.SOLICITADA,
                "requester-7",
                Instant.parse("2026-11-02T22:00:00Z"),
                "Solicitação criada");

        assertEquals(null, created.from());
        assertEquals(ReservationStatus.SOLICITADA, created.to());

        assertThrows(IllegalArgumentException.class, () -> new ReservationTransition(
                "reservation-42",
                null,
                ReservationStatus.APROVADA,
                "responsible-7",
                Instant.parse("2026-11-02T22:00:00Z"),
                "Aprovação sem estado anterior"));
    }
}
