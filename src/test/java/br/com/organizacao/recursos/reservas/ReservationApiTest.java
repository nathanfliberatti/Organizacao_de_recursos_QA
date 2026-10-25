package br.com.organizacao.recursos.reservas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReservationController.class)
@Import({ReservationCatalog.class, MaintenanceCatalog.class, InMemoryReservationStore.class,
        ReservationSchedulingService.class,
    ApplicationClockConfiguration.class})
class ReservationApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationCatalog reservationCatalog;
    @Autowired
    private MaintenanceCatalog maintenanceCatalog;
        @Autowired
        private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "requester-1", roles = "SOLICITANTE")
    void restrictedReservationApiReturnsSolicitadaBeforeResponsibleDecision() throws Exception {
        mockMvc.perform(get("/api/reservations/restricted"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLICITADA"));
    }

    @Test
    @WithMockUser(username = "requester-1", roles = "RESPONSAVEL")
    void apiRejectsRequesterApprovingOwnRestrictedReservation() throws Exception {
        String reservationId = reservationCatalog.createRestrictedReservation("requester-1");

        mockMvc.perform(post("/api/reservations/{id}/approval", reservationId).with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status").value("SOLICITADA"))
                .andExpect(jsonPath("$.approvalDecision").value(nullValue()));
    }

    @Test
    @WithMockUser(username = "requester-1", roles = "SOLICITANTE")
    void apiRejectsReservationCrossingMaintenanceAndDoesNotStoreIt() throws Exception {
        maintenanceCatalog.block("room-1", new MaintenancePeriod(
                Instant.parse("2026-10-25T10:00:00Z"),
                Instant.parse("2026-10-25T11:00:00Z")));
        int reservationsBefore = reservationCatalog.count();
        CreateReservationRequest request = new CreateReservationRequest(
                "room-1",
                Instant.parse("2026-10-25T09:59:59Z"),
                Instant.parse("2026-10-25T10:00:01Z"));

        mockMvc.perform(post("/api/reservations")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.accepted").value(false));

        assertEquals(reservationsBefore, reservationCatalog.count());
    }

    @Test
    @WithMockUser(username = "responsible-1", roles = "RESPONSAVEL")
    void apiRunsMainLifecycleAndReturnsThreeAuditTransitions() throws Exception {
        String reservationId = reservationCatalog.createRestrictedReservation("requester-1");

        mockMvc.perform(post("/api/reservations/{id}/approval", reservationId).with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status").value("APROVADA"));
        mockMvc.perform(post("/api/reservations/{id}/start", reservationId).with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status").value("EM_USO"));
        mockMvc.perform(post("/api/reservations/{id}/complete", reservationId).with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.transitions.length()").value(3))
                .andExpect(jsonPath("$.transitions[0].from").value("SOLICITADA"))
                .andExpect(jsonPath("$.transitions[2].to").value("CONCLUIDA"));
    }
}
