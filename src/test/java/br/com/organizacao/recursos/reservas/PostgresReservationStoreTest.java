package br.com.organizacao.recursos.reservas;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class PostgresReservationStoreTest {
        private final StubJdbcTemplate jdbcTemplate = new StubJdbcTemplate();
    private final PostgresReservationStore store = new PostgresReservationStore(jdbcTemplate);
    private final ReservationPeriod period = new ReservationPeriod(
            Instant.parse("2026-10-25T10:00:00Z"), Instant.parse("2026-10-25T11:00:00Z"));

    @Test
    void doesNotInsertWhenReservationIsUnavailable() {
                jdbcTemplate.queryResult = false;

        assertFalse(store.schedule("room-1", "professor-1", "requester-1", period));

                assertFalse(jdbcTemplate.inserted);
    }

    @Test
    void insertsWhenReservationIsAvailable() {
        assertTrue(store.schedule("room-1", "professor-1", "requester-1", period));
                assertTrue(jdbcTemplate.inserted);
    }

    @ParameterizedTest
    @ValueSource(strings = {"23P01", "40P01"})
    void returnsConflictWhenPostgresReportsReservationConflict(String sqlState) {
                jdbcTemplate.updateFailure = new DataIntegrityViolationException(
                                "Reservation conflict", new SQLException("Reservation conflict", sqlState));

        assertFalse(store.schedule("room-1", "professor-1", "requester-1", period));
    }

    @Test
    void propagatesUnexpectedDatabaseErrors() {
                jdbcTemplate.updateFailure = new DataAccessResourceFailureException("Database unavailable");

        assertThrows(DataAccessResourceFailureException.class,
                () -> store.schedule("room-1", "professor-1", "requester-1", period));
    }

        private static class StubJdbcTemplate extends JdbcTemplate {
                private boolean queryResult = true;
                private boolean inserted;
                private RuntimeException updateFailure;

                @Override
                public <T> T execute(org.springframework.jdbc.core.ConnectionCallback<T> action) {
                        return null;
                }

                @Override
                public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
                        return requiredType.cast(queryResult);
                }

                @Override
                public int update(String sql, Object... args) {
                        if (updateFailure != null) {
                                throw updateFailure;
                        }
                        inserted = true;
                        return 1;
                }
        }
}