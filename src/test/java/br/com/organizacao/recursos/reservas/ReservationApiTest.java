package br.com.organizacao.recursos.reservas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReservationController.class)
class ReservationApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "requester-1", roles = "SOLICITANTE")
    void restrictedReservationApiReturnsSolicitadaBeforeResponsibleDecision() throws Exception {
        mockMvc.perform(get("/api/reservations/restricted"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLICITADA"));
    }
}
