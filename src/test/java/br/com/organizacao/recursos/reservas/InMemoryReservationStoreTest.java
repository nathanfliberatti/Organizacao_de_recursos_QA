package br.com.organizacao.recursos.reservas;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class InMemoryReservationStoreTest {
    @Test
    void acceptsReservationsAndReportsAvailability() {
        InMemoryReservationStore store = new InMemoryReservationStore();
        ReservationPeriod period = new ReservationPeriod(
                Instant.parse("2026-10-25T10:00:00Z"), Instant.parse("2026-10-25T11:00:00Z"));

        assertTrue(store.schedule("room-1", "professor-1", "requester-1", period));
        assertTrue(store.isAvailable("room-1", "professor-1", period));
        assertEquals("requester-1", store.requesterId(1).orElseThrow());
        assertTrue(store.requesterId(99).isEmpty());

        store.update(1, new ReservationPeriod(period.startsAt(), period.endsAt().plusSeconds(3600)));
        assertEquals("requester-1", store.requesterId(1).orElseThrow());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> store.update(99, period)).getStatusCode());
        store.recordConflictAttempt("room-1", "professor-1", period);
    }
}