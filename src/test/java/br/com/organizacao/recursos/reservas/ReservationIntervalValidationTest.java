package br.com.organizacao.recursos.reservas;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/** RF-13: o término da reserva deve ser posterior ao início. */
@WebMvcTest(ReservationController.class)
@Import({ReservationCatalog.class, MaintenanceCatalog.class, InMemoryReservationStore.class,
        ReservationSchedulingService.class, InMemoryReservationAuditService.class,
        ApplicationClockConfiguration.class})
class ReservationIntervalValidationTest {
    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = "{0} -> {1} responde {2}")
    @CsvSource({
            "2027-01-01T10:00:00Z, 2027-01-01T10:00:00Z, 400",
            "2027-01-01T10:00:00Z, 2027-01-01T09:59:59Z, 400",
            "2027-01-01T10:00:00Z, 2027-01-01T09:00:00Z, 400",
            "2027-01-02T10:00:00Z, 2027-01-01T10:00:00Z, 400",
            "2027-01-01T10:00:00Z, 2027-01-01T10:00:01Z, 201",
            "2027-01-01T10:00:00Z, 2027-01-01T11:00:00Z, 201"
    })
    @WithMockUser(username = "requester-1", roles = "SOLICITANTE")
    void rejectsEqualOrInvertedIntervalAndAcceptsValidOne(String startsAt, String endsAt, int expectedStatus)
            throws Exception {
        mockMvc.perform(post("/api/reservations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":\"room-" + startsAt.hashCode() + endsAt.hashCode()
                                + "\",\"startsAt\":\"" + startsAt + "\",\"endsAt\":\"" + endsAt + "\"}"))
                .andExpect(status().is(expectedStatus));
    }
}
