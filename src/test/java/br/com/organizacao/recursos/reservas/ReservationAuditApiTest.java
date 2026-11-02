package br.com.organizacao.recursos.reservas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
@WebMvcTest(ReservationController.class)
@Import({ReservationSecurityConfiguration.class, ReservationCatalog.class,
        MaintenanceCatalog.class, InMemoryReservationStore.class,
        ReservationSchedulingService.class, InMemoryReservationAuditService.class,
        ApplicationClockConfiguration.class})
class ReservationAuditApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryReservationAuditService reservationAuditService;

    @BeforeEach
    void clearAudit() {
        reservationAuditService.clear();
    }

    @Test
    void responsibleCanReadReservationHistory() throws Exception {
        createApprovedReservation();

        mockMvc.perform(get("/api/reservations/reservation-42/history")
                        .with(user("responsible-7").roles("RESPONSAVEL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reservationId").value("reservation-42"))
                .andExpect(jsonPath("$[1].actorId").value("responsible-7"))
                .andExpect(jsonPath("$[1].reason").value("Aprovada pelo responsável"));
    }

    @Test
    void administratorCanReadReservationHistory() throws Exception {
        reservationAuditService.createReservation(
                "reservation-42", "requester-1", Clock.fixed(
                        Instant.parse("2026-11-02T22:00:00Z"), ZoneOffset.UTC), "Solicitação criada");

        mockMvc.perform(get("/api/reservations/reservation-42/history")
                        .with(user("admin-1").roles("ADMINISTRADOR")))
                .andExpect(status().isOk());
    }

    @Test
    void requesterReceivesForbiddenWhenReadingReservationHistory() throws Exception {
        reservationAuditService.createReservation(
                "reservation-42", "requester-1", Clock.fixed(
                        Instant.parse("2026-11-02T22:00:00Z"), ZoneOffset.UTC), "Solicitação criada");

        mockMvc.perform(get("/api/reservations/reservation-42/history")
                        .with(user("requester-1").roles("SOLICITANTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditHistoryHasNoApiMutationOperations() throws Exception {
        reservationAuditService.createReservation(
                "reservation-42", "requester-1", Clock.fixed(
                        Instant.parse("2026-11-02T22:00:00Z"), ZoneOffset.UTC), "Solicitação criada");

        mockMvc.perform(put("/api/reservations/reservation-42/history")
                        .with(user("admin-1").roles("ADMINISTRADOR")).with(csrf()))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(patch("/api/reservations/reservation-42/history")
                        .with(user("admin-1").roles("ADMINISTRADOR")).with(csrf()))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/reservations/reservation-42/history")
                        .with(user("admin-1").roles("ADMINISTRADOR")).with(csrf()))
                .andExpect(status().isMethodNotAllowed());
        assertEquals(1, reservationAuditService.history("reservation-42").size());
    }

    @Test
    void deletingStartedReservationReturnsConflict() throws Exception {
        createApprovedReservation();
        reservationAuditService.changeState(
                "reservation-42",
                ReservationStatus.EM_USO,
                "responsible-7",
                Clock.fixed(Instant.parse("2026-11-02T22:00:00Z"), ZoneOffset.UTC),
                "Uso iniciado");

        mockMvc.perform(delete("/api/reservations/reservation-42")
                        .with(user("responsible-7").roles("RESPONSAVEL")).with(csrf()))
                .andExpect(status().isConflict());
    }

    private void createApprovedReservation() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-02T22:00:00Z"), ZoneOffset.UTC);
        reservationAuditService.createReservation(
                "reservation-42", "requester-1", clock, "Solicitação criada");
        reservationAuditService.changeState(
                "reservation-42",
                ReservationStatus.APROVADA,
                "responsible-7",
                clock,
                "Aprovada pelo responsável");
    }
}
