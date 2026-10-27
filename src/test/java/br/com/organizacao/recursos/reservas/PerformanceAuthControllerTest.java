package br.com.organizacao.recursos.reservas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration")
@AutoConfigureMockMvc
@ActiveProfiles("perf")
class PerformanceAuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginIssuesBearerTokenAcceptedByProtectedAvailabilityEndpoint() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"perf-user-01","password":"local-only-placeholder-01"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response)
                .get("access_token")
                .asText();

        mockMvc.perform(get("/api/resources/room-shared/availability")
                        .queryParam("startsAt", "2026-10-27T10:00:00Z")
                        .queryParam("endsAt", "2026-10-27T11:00:00Z")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void loginRejectsInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"perf-user-01","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void availabilityRejectsRequestsWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/api/resources/room-shared/availability")
                        .queryParam("startsAt", "2026-10-27T10:00:00Z")
                        .queryParam("endsAt", "2026-10-27T11:00:00Z"))
                .andExpect(status().isUnauthorized());
    }
}