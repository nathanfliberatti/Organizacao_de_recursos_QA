package br.com.organizacao.recursos.reservas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@ActiveProfiles("postgres")
class ReservationAuditIT {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("resource_reservations")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private ReservationAuditService reservationAuditService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-11-02T22:00:00Z"), ZoneOffset.UTC);

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @BeforeEach
    void clearAuditTables() {
        jdbcTemplate.update("DELETE FROM reservation_audit");
        jdbcTemplate.update("DELETE FROM reservation_lifecycle");
    }

    @Test
    void stateAndAuditArePersistedTogether() {
        reservationAuditService.createReservation(
                "reservation-42", "requester-1", clock, "Solicitação criada");
        reservationAuditService.changeState(
                "reservation-42",
                ReservationStatus.APROVADA,
                "responsible-1",
                clock,
                "Aprovada pelo responsável");

        assertEquals("APROVADA", jdbcTemplate.queryForObject(
                "SELECT status FROM reservation_lifecycle WHERE reservation_id = ?",
                String.class,
                "reservation-42"));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservation_audit WHERE reservation_id = ?",
                Integer.class,
                "reservation-42"));
        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM reservation_audit
                WHERE reservation_id = ? AND previous_status = 'SOLICITADA'
                  AND new_status = 'APROVADA' AND user_id = ?
                  AND occurred_at = ? AND reason = ?
                """,
                Integer.class,
                "reservation-42",
                "responsible-1",
                java.sql.Timestamp.from(clock.instant()),
                "Aprovada pelo responsável"));
    }

    @Test
    void rollbackDoesNotLeaveStateChangeOrOrphanedAudit() {
        reservationAuditService.createReservation(
                "reservation-rollback", "requester-1", clock, "Solicitação criada");
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThrows(IllegalStateException.class, () -> transaction.execute(status -> {
            reservationAuditService.changeState(
                    "reservation-rollback",
                    ReservationStatus.APROVADA,
                    "responsible-1",
                    clock,
                    "Aprovada pelo responsável");
            throw new IllegalStateException("Forçar rollback da transação externa");
        }));

        assertEquals("SOLICITADA", jdbcTemplate.queryForObject(
                "SELECT status FROM reservation_lifecycle WHERE reservation_id = ?",
                String.class,
                "reservation-rollback"));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservation_audit WHERE reservation_id = ?",
                Integer.class,
                "reservation-rollback"));
        assertEquals(0, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM reservation_audit
                WHERE reservation_id = ? AND new_status = 'APROVADA'
                """,
                Integer.class,
                "reservation-rollback"));
    }

    @Test
    void startedReservationCannotBeDeletedAndItsHistoryRemains() {
        reservationAuditService.createReservation(
                "reservation-started", "requester-1", clock, "Solicitação criada");
        reservationAuditService.changeState(
                "reservation-started",
                ReservationStatus.APROVADA,
                "responsible-1",
                clock,
                "Aprovada pelo responsável");
        reservationAuditService.changeState(
                "reservation-started",
                ReservationStatus.EM_USO,
                "responsible-1",
                clock,
                "Uso iniciado");

        assertThrows(ReservationDeletionConflictException.class,
                () -> reservationAuditService.deleteReservation("reservation-started"));

        assertEquals(false, jdbcTemplate.queryForObject(
                "SELECT deleted FROM reservation_lifecycle WHERE reservation_id = ?",
                Boolean.class,
                "reservation-started"));
        assertEquals(3, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservation_audit WHERE reservation_id = ?",
                Integer.class,
                "reservation-started"));
    }
}
