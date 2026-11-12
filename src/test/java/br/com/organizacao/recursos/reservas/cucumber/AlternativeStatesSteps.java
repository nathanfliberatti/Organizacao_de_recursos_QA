package br.com.organizacao.recursos.reservas.cucumber;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import io.cucumber.spring.ScenarioScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Passos dos cenários de estados alternativos (RF-14), pela API HTTP com identidades JWT de teste. */
@ScenarioScope
public class AlternativeStatesSteps {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String reservationId;
    private int lastStatus;

    private static RequestPostProcessor identity(String username, String role) {
        return jwt().jwt(token -> token.subject(username))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private JsonNode send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        MvcResult result = mockMvc.perform(request).andReturn();
        lastStatus = result.getResponse().getStatus();
        String body = result.getResponse().getContentAsString();
        return body.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(body);
    }

    private void createRequestBy(String requester) throws Exception {
        String role = requester.startsWith("responsavel") ? "RESPONSAVEL" : "SOLICITANTE";
        reservationId = send(post("/api/reservations/restricted").with(identity(requester, role))).get("id").asText();
        assertEquals(201, lastStatus);
    }

    @Dado("que o solicitante {string} criou uma solicitação de recurso restrito")
    public void requesterCreatedRestrictedRequest(String requester) throws Exception {
        createRequestBy(requester);
    }

    @Dado("que a reserva do solicitante {string} foi aprovada")
    public void reservationWasApproved(String requester) throws Exception {
        createRequestBy(requester);
        send(post("/api/reservations/{id}/approval", reservationId).with(identity("responsavel-1", "RESPONSAVEL")));
        assertEquals(200, lastStatus);
    }

    @Dado("que a reserva do solicitante {string} foi aprovada e iniciada")
    public void reservationWasApprovedAndStarted(String requester) throws Exception {
        reservationWasApproved(requester);
        send(post("/api/reservations/{id}/start", reservationId).with(identity(requester, "SOLICITANTE")));
        assertEquals(200, lastStatus);
    }

    @E("que o usuário {string} com perfil {string} rejeitou a reserva")
    public void userRejectedTheReservation(String user, String role) throws Exception {
        userRejects(user, role);
    }

    @Quando("o usuário {string} com perfil {string} rejeita a reserva")
    public void userRejects(String user, String role) throws Exception {
        send(post("/api/reservations/{id}/rejection", reservationId).with(identity(user, role)));
    }

    @Quando("o usuário {string} com perfil {string} cancela a reserva")
    public void userCancels(String user, String role) throws Exception {
        send(delete("/api/reservations/{id}", reservationId).with(identity(user, role)));
    }

    @Quando("o usuário {string} com perfil {string} registra o não comparecimento")
    public void userRegistersNoShow(String user, String role) throws Exception {
        send(post("/api/reservations/{id}/no-show", reservationId).with(identity(user, role)));
    }

    @Quando("o usuário {string} com perfil {string} tenta aprovar a reserva")
    public void userTriesToApprove(String user, String role) throws Exception {
        send(post("/api/reservations/{id}/approval", reservationId).with(identity(user, role)));
    }

    @Entao("a resposta deve ter status {int}")
    public void responseStatusIs(int expected) {
        assertEquals(expected, lastStatus);
    }

    @Entao("a reserva deve estar no estado {string}")
    public void reservationIsInState(String expected) throws Exception {
        JsonNode body = send(get("/api/reservations/{id}", reservationId).with(identity("responsavel-9", "RESPONSAVEL")));
        assertEquals(200, lastStatus);
        assertEquals(expected, body.get("status").asText());
    }

    @Entao("a auditoria deve registrar a transição de {string} para {string} feita por {string}")
    public void auditRecordsTransition(String from, String to, String actor) throws Exception {
        JsonNode history = send(get("/api/reservations/{id}/history", reservationId)
                .with(identity("responsavel-9", "RESPONSAVEL")));
        JsonNode last = history.get(history.size() - 1);
        assertEquals(from, last.get("from").asText());
        assertEquals(to, last.get("to").asText());
        assertEquals(actor, last.get("actorId").asText());
    }
}
