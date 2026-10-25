package br.com.organizacao.recursos.reservas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReservationController.class)
@Import({ReservationCatalog.class, ApplicationClockConfiguration.class})
class ReservationApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationCatalog reservationCatalog;

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
}
