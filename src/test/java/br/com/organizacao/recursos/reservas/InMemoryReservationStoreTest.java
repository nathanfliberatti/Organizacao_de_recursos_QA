package br.com.organizacao.recursos.reservas;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class InMemoryReservationStoreTest {
    @Test
    void acceptsReservationsAndReportsAvailability() {
        InMemoryReservationStore store = new InMemoryReservationStore();
        ReservationPeriod period = new ReservationPeriod(
                Instant.parse("2026-10-25T10:00:00Z"), Instant.parse("2026-10-25T11:00:00Z"));

        assertTrue(store.schedule("room-1", "professor-1", "requester-1", period));
        assertTrue(store.isAvailable("room-1", "professor-1", period));
    }
}