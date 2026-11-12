package br.com.organizacao.recursos.reservas;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class InMemoryReservationStoreTest {
    private static ReservationPeriod period(String from, String to) {
        return new ReservationPeriod(Instant.parse(from), Instant.parse(to));
    }

    @Test
    void acceptsReservationsAndReportsAvailability() {
        InMemoryReservationStore store = new InMemoryReservationStore();
        ReservationPeriod period = period("2026-10-25T10:00:00Z", "2026-10-25T11:00:00Z");

        assertTrue(store.isAvailable("room-1", "professor-1", period));
        assertTrue(store.schedule("room-1", "professor-1", "requester-1", period));
        assertFalse(store.isAvailable("room-1", "professor-1", period));
        assertEquals("requester-1", store.requesterId(1).orElseThrow());
        assertTrue(store.requesterId(99).isEmpty());

        store.update(1, new ReservationPeriod(period.startsAt(), period.endsAt().plusSeconds(3600)));
        assertEquals("requester-1", store.requesterId(1).orElseThrow());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> store.update(99, period)).getStatusCode());
        store.recordConflictAttempt("room-1", "professor-1", period);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "mesmo horário, 2026-10-25T10:00:00Z, 2026-10-25T11:00:00Z, true",
            "começa no meio, 2026-10-25T10:30:00Z, 2026-10-25T11:30:00Z, true",
            "termina no meio, 2026-10-25T09:30:00Z, 2026-10-25T10:30:00Z, true",
            "contém a outra, 2026-10-25T09:00:00Z, 2026-10-25T12:00:00Z, true",
            "começa quando a outra termina, 2026-10-25T11:00:00Z, 2026-10-25T12:00:00Z, false",
            "termina quando a outra começa, 2026-10-25T09:00:00Z, 2026-10-25T10:00:00Z, false",
            "bem depois, 2026-10-25T15:00:00Z, 2026-10-25T16:00:00Z, false"
    })
    void rejectsOverlapForTheSameRoomAndTouchingIntervalsDoNotConflict(
            String name, String from, String to, boolean overlaps) {
        InMemoryReservationStore store = new InMemoryReservationStore();
        assertTrue(store.schedule("room-1", "professor-1", "requester-1",
                period("2026-10-25T10:00:00Z", "2026-10-25T11:00:00Z")));

        assertEquals(!overlaps, store.schedule("room-1", "professor-2", "requester-2", period(from, to)));
    }

    @Test
    void rejectsOverlapForTheSameProfessorInAnotherRoomButNotForAnotherProfessor() {
        InMemoryReservationStore store = new InMemoryReservationStore();
        ReservationPeriod slot = period("2026-10-25T10:00:00Z", "2026-10-25T11:00:00Z");
        assertTrue(store.schedule("room-1", "professor-1", "requester-1", slot));

        assertFalse(store.schedule("room-2", "professor-1", "requester-2", slot));
        assertTrue(store.schedule("room-2", "professor-2", "requester-2", slot));
    }

    @Test
    void cancelledReservationReleasesTheSlotAndUpdateRespectsOtherReservations() {
        InMemoryReservationStore store = new InMemoryReservationStore();
        ReservationPeriod first = period("2026-10-25T10:00:00Z", "2026-10-25T11:00:00Z");
        ReservationPeriod second = period("2026-10-25T11:00:00Z", "2026-10-25T12:00:00Z");
        store.schedule("room-1", "professor-1", "requester-1", first);
        store.schedule("room-1", "professor-2", "requester-2", second);

        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> store.update(2, period("2026-10-25T10:30:00Z", "2026-10-25T11:30:00Z"))).getStatusCode());

        assertTrue(store.cancel(1));
        assertFalse(store.cancel(1));
        assertTrue(store.requesterId(1).isEmpty());
        assertTrue(store.isAvailable("room-1", "professor-3", first));
        store.update(2, period("2026-10-25T10:30:00Z", "2026-10-25T11:30:00Z"));
    }

    @Test
    void concurrentRequestsForTheSameSlotConfirmOnlyOneReservation() throws Exception {
        InMemoryReservationStore store = new InMemoryReservationStore();
        ReservationPeriod slot = period("2026-10-25T10:00:00Z", "2026-10-25T11:00:00Z");
        int workers = 16;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(workers);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        try {
            List<java.util.concurrent.Future<Boolean>> results = new java.util.ArrayList<>();
            for (int i = 0; i < workers; i++) {
                String professor = "professor-" + i;
                results.add(executor.submit(() -> {
                    start.await();
                    return store.schedule("room-shared", professor, "requester", slot);
                }));
            }
            start.countDown();
            long accepted = 0;
            for (java.util.concurrent.Future<Boolean> result : results) {
                accepted += result.get() ? 1 : 0;
            }
            assertEquals(1, accepted);
        } finally {
            executor.shutdownNow();
        }
    }
}
