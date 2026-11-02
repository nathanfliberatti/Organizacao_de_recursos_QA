package br.com.organizacao.recursos.reservas;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import br.com.organizacao.recursos.reservas.cucumber.CucumberMethodSecurityConfiguration;

@WebMvcTest(ReservationController.class)
@Import(CucumberMethodSecurityConfiguration.class)
class ReservationAuditApiTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservationCatalog reservationCatalog;

    @MockBean
    private ReservationSchedulingService reservationSchedulingService;

    @MockBean
    private ReservationAuditService reservationAuditService;

    @Test
    void responsibleCanReadReservationHistory() throws Exception {
        given(reservationAuditService.history("reservation-42")).willReturn(List.of(
                new ReservationTransition(
                        "reservation-42",
                        ReservationStatus.SOLICITADA,
                        ReservationStatus.APROVADA,
                        "responsible-7",
                        Instant.parse("2026-11-02T22:00:00Z"),
                        "Aprovada pelo responsável")));

        mockMvc.perform(get("/api/reservations/reservation-42/history")
                        .with(user("responsible-7").roles("RESPONSAVEL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reservationId").value("reservation-42"))
                .andExpect(jsonPath("$[0].actorId").value("responsible-7"))
                .andExpect(jsonPath("$[0].reason").value("Aprovada pelo responsável"));
    }

    @Test
    void administratorCanReadReservationHistory() throws Exception {
        given(reservationAuditService.history("reservation-42")).willReturn(List.of());

        mockMvc.perform(get("/api/reservations/reservation-42/history")
                        .with(user("admin-1").roles("ADMINISTRADOR")))
                .andExpect(status().isOk());
    }

    @Test
    void requesterReceivesForbiddenWhenReadingReservationHistory() throws Exception {
        mockMvc.perform(get("/api/reservations/reservation-42/history")
                        .with(user("requester-1").roles("SOLICITANTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditHistoryHasNoApiMutationOperations() throws Exception {
        mockMvc.perform(put("/api/reservations/reservation-42/history")
                        .with(user("admin-1").roles("ADMINISTRADOR")))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(patch("/api/reservations/reservation-42/history")
                        .with(user("admin-1").roles("ADMINISTRADOR")))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/reservations/reservation-42/history")
                        .with(user("admin-1").roles("ADMINISTRADOR")))
                .andExpect(status().isMethodNotAllowed());
    }
}
