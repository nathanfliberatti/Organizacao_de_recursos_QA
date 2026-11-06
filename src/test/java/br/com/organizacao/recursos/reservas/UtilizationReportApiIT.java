package br.com.organizacao.recursos.reservas;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("postgres")
class UtilizationReportApiIT {
    private static final Instant START = Instant.parse("2026-11-02T10:00:00Z");
    private static final Instant END = Instant.parse("2026-11-02T12:00:00Z");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("resource_reservations")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("RECURSOS_JWT_SECRET", () -> "utilization-test-secret-with-at-least-32-bytes");
        registry.add("BOOTSTRAP_ADMIN_LOGIN", () -> "utilization-admin");
        registry.add("BOOTSTRAP_ADMIN_PASSWORD", () -> "utilization-password");
    }

    @BeforeEach
    void clearReportData() {
        jdbcTemplate.update("DELETE FROM reservation_conflict_attempts");
        jdbcTemplate.update("DELETE FROM reservations");
    }

    @Test
    @WithMockUser(username = "responsible-1", roles = "RESPONSAVEL")
    void reportsClippedHoursAndOverlappingDeclinedAttempts() throws Exception {
        insertReservation("resource-a", "professor-a",
                "2026-11-02T09:00:00Z", "2026-11-02T11:00:00Z", "AGENDADA");
        insertReservation("resource-b", "professor-b",
                "2026-11-02T10:30:00Z", "2026-11-02T11:30:00Z", "AGENDADA");
        insertReservation("resource-c", "professor-c",
                "2026-11-02T10:00:00Z", "2026-11-02T11:00:00Z", "CANCELADA");
        insertConflict("resource-x", "professor-x",
                "2026-11-02T09:30:00Z", "2026-11-02T10:15:00Z");
        insertConflict("resource-y", "professor-y",
                "2026-11-02T12:00:00Z", "2026-11-02T13:00:00Z");

        // Manual: resource-a/professor-a contributes 1 h after clipping at 10:00;
        // resource-b/professor-b contributes 1 h; resource-c is cancelled and contributes 0 h.
        // One seeded conflict overlaps the period; the 12:00 attempt only touches its exclusive end.
        // The rejected POST below adds one more overlapping conflict, for 2 avoided conflicts total.
        mockMvc.perform(post("/api/reservations")
                        .with(user("requester-1").roles("SOLICITANTE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resourceId": "resource-a",
                                  "startsAt": "2026-11-02T10:15:00Z",
                                  "endsAt": "2026-11-02T10:45:00Z"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.accepted").value(false));

        mockMvc.perform(get("/relatorios/utilizacao")
                        .with(user("responsible-1").roles("RESPONSAVEL"))
                        .queryParam("inicio", START.toString())
                        .queryParam("fim", END.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horasPorRecurso.length()").value(2))
                .andExpect(jsonPath("$.horasPorRecurso[0].recursoId").value("resource-a"))
                .andExpect(jsonPath("$.horasPorRecurso[0].horasReservadas").value(1.0))
                .andExpect(jsonPath("$.horasPorRecurso[1].recursoId").value("resource-b"))
                .andExpect(jsonPath("$.horasPorRecurso[1].horasReservadas").value(1.0))
                .andExpect(jsonPath("$.cargaPorProfessor.length()").value(2))
                .andExpect(jsonPath("$.cargaPorProfessor[0].professorId").value("professor-a"))
                .andExpect(jsonPath("$.cargaPorProfessor[0].horasAlocadas").value(1.0))
                .andExpect(jsonPath("$.cargaPorProfessor[1].professorId").value("professor-b"))
                .andExpect(jsonPath("$.cargaPorProfessor[1].horasAlocadas").value(1.0))
                .andExpect(jsonPath("$.conflitosEvitados").value(2));
    }

    @Test
    @WithMockUser(username = "responsible-1", roles = "RESPONSAVEL")
    void emptyPeriodReturnsNoUsageOrConflicts() throws Exception {
        // Manual: no seeded reservation or declined attempt overlaps this interval; every total is 0.
        mockMvc.perform(get("/relatorios/utilizacao")
                        .queryParam("inicio", START.toString())
                        .queryParam("fim", END.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horasPorRecurso.length()").value(0))
                .andExpect(jsonPath("$.cargaPorProfessor.length()").value(0))
                .andExpect(jsonPath("$.conflitosEvitados").value(0));
    }

    @Test
    @WithMockUser(username = "responsible-1", roles = "RESPONSAVEL")
    void cancelledReservationsDoNotCountAsUsage() throws Exception {
        insertReservation("resource-c", "professor-c",
                "2026-11-02T10:00:00Z", "2026-11-02T11:00:00Z", "CANCELADA");

        // Manual: the only reservation is cancelled, so resource hours and professor workload are both 0.
        mockMvc.perform(get("/relatorios/utilizacao")
                        .queryParam("inicio", START.toString())
                        .queryParam("fim", END.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horasPorRecurso.length()").value(0))
                .andExpect(jsonPath("$.cargaPorProfessor.length()").value(0));
    }

    @Test
    void reportRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/relatorios/utilizacao")
                        .queryParam("inicio", START.toString())
                        .queryParam("fim", END.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "requester-1", roles = "SOLICITANTE")
    void requesterCannotReadReport() throws Exception {
        mockMvc.perform(get("/relatorios/utilizacao")
                        .queryParam("inicio", START.toString())
                        .queryParam("fim", END.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin-1", roles = "ADMINISTRADOR")
    void administratorCanReadReport() throws Exception {
        mockMvc.perform(get("/relatorios/utilizacao")
                        .queryParam("inicio", START.toString())
                        .queryParam("fim", END.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "responsible-1", roles = "RESPONSAVEL")
    void rejectsAnEmptyOrReversedPeriod() throws Exception {
        mockMvc.perform(get("/relatorios/utilizacao")
                        .queryParam("inicio", END.toString())
                        .queryParam("fim", START.toString()))
                .andExpect(status().isBadRequest());
    }

    private void insertReservation(
            String resourceId, String professorId, String startsAt, String endsAt, String status) {
        jdbcTemplate.update("""
                INSERT INTO reservations (room_id, professor_id, requester_id, starts_at, ends_at, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                resourceId,
                professorId,
                "requester-" + professorId,
                Timestamp.from(Instant.parse(startsAt)),
                Timestamp.from(Instant.parse(endsAt)),
                status);
    }

    private void insertConflict(String resourceId, String professorId, String startsAt, String endsAt) {
        jdbcTemplate.update("""
                INSERT INTO reservation_conflict_attempts (room_id, professor_id, starts_at, ends_at)
                VALUES (?, ?, ?, ?)
                """,
                resourceId,
                professorId,
                Timestamp.from(Instant.parse(startsAt)),
                Timestamp.from(Instant.parse(endsAt)));
    }
}
