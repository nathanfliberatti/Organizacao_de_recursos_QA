package br.com.organizacao.recursos.reservas.cucumber;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import br.com.organizacao.recursos.reservas.CreateReservationRequest;
import br.com.organizacao.recursos.reservas.MaintenanceCatalog;
import br.com.organizacao.recursos.reservas.MaintenancePeriod;
import br.com.organizacao.recursos.reservas.ReservationCatalog;
import br.com.organizacao.recursos.reservas.ReservationResponse;
import br.com.organizacao.recursos.reservas.ReservationStatus;
import br.com.organizacao.recursos.reservas.ReservationTransition;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import io.cucumber.spring.ScenarioScope;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ScenarioScope
public class ApprovalBlockingSteps {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReservationCatalog reservationCatalog;

    @Autowired
    private MaintenanceCatalog maintenanceCatalog;

    private String reservationId;
    private CreateReservationRequest requestedPeriod;
    private ReservationResponse currentResponse;
    private boolean accepted;
    private boolean available;
    private String reservationCreatorId;
    private String reservationCreatorRole;

    @Before
    public void resetScenarioState() {
        reservationCatalog.clear();
        maintenanceCatalog.clear();
        reservationId = null;
        requestedPeriod = null;
        currentResponse = null;
        accepted = false;
        available = true;
        reservationCreatorId = null;
        reservationCreatorRole = null;
    }

    @Dado("que uma reserva solicitada utiliza um recurso classificado como restrito")
    public void restrictedResourceReservationIsCreated() throws Exception {
        createRestrictedReservation("requester-1", "SOLICITANTE");
    }

    @Dado("que a decisão de um Responsável ainda não foi registrada")
    public void responsibleDecisionHasNotBeenRecorded() {
        assertEquals(ReservationStatus.SOLICITADA, reservationCatalog.findById(reservationId).status());
    }

    @Quando("o sistema consulta o estado da reserva")
    public void queryReservationState() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/reservations/{id}", reservationId)
                        .with(profileToken("requester-1", "SOLICITANTE")))
            .andExpect(status().is2xxSuccessful())
            .andReturn();
        currentResponse = readReservationResponse(result);
    }

    @Entao("a reserva não deve ser tratada como aprovada")
    public void reservationMustNotBeTreatedAsApproved() {
        assertNotEquals(ReservationStatus.APROVADA.name(), currentResponse.status());
    }

    @Dado("que existe uma solicitação pendente para um recurso restrito")
    public void pendingRestrictedReservationExists() throws Exception {
        createRestrictedReservation("requester-1", "SOLICITANTE");
    }

    @Quando("um usuário com perfil Responsável aprova a solicitação")
    public void responsibleApprovesRequest() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/reservations/{id}/approval", reservationId)
                        .with(profileToken("responsible-1", "RESPONSAVEL"))
                        .with(csrf()))
            .andExpect(status().is2xxSuccessful())
                .andReturn();
        currentResponse = readReservationResponse(result);
    }

    @Entao("a decisão deve ser registrada")
    public void approvalDecisionMustBeRecorded() {
        assertNotNull(currentResponse.approvalDecision());
    }

    @Entao("a reserva deve refletir a aprovação")
    public void reservationMustReflectApproval() {
        assertEquals(ReservationStatus.APROVADA.name(), currentResponse.status());
    }

    @Entao("^a mudança de estado deve gerar auditoria com ação, reserva, usuário e data/hora, conforme RF-10$")
    public void statusChangeMustBeAudited() {
        ReservationTransition transition = currentResponse.transitions().getFirst();
        assertEquals(ReservationStatus.SOLICITADA, transition.from());
        assertEquals(ReservationStatus.APROVADA, transition.to());
        assertEquals("responsible-1", transition.actorId());
        assertNotNull(transition.changedAt());
    }

    @Dado("que a reserva restrita foi criada pela identidade autenticada")
    public void reservationWasCreatedByAuthenticatedIdentity() throws Exception {
        createRestrictedReservation("same-user", "RESPONSAVEL");
    }

    @Dado("que essa identidade possui perfil Responsável")
    public void sameIdentityHasResponsibleProfile() {
        assertEquals("same-user", reservationCreatorId);
        assertEquals("RESPONSAVEL", reservationCreatorRole);
    }

    @Quando("ela tenta aprovar a própria reserva pela API")
    public void sameIdentityAttemptsSelfApproval() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/reservations/{id}/approval", reservationId)
                        .with(profileToken("same-user", "RESPONSAVEL"))
                        .with(csrf()))
                .andReturn();
        currentResponse = readReservationResponse(result);
    }

    @Entao("a resposta não deve indicar aprovação")
    public void responseMustNotIndicateApproval() {
        assertNotEquals(ReservationStatus.APROVADA.name(), currentResponse.status());
    }

    @Entao("o estado deve permanecer {string}")
    public void stateMustRemain(String expectedStatus) {
        assertEquals(expectedStatus, currentResponse.status());
    }

    @Entao("a decisão deve permanecer ausente")
    public void decisionMustRemainAbsent() {
        assertNull(currentResponse.approvalDecision());
    }

    @Dado("que uma reserva restrita aguarda decisão e o usuário autenticado tem perfil Solicitante")
    public void restrictedReservationWaitsForDecisionAndRequesterProfileIsAuthenticated() throws Exception {
        createRestrictedReservation("requester-1", "SOLICITANTE");
    }

    @Dado("que sua identidade é diferente da identidade que criou a reserva")
    public void requesterIdentityDiffersFromCreator() {
        assertNotEquals("requester-2", reservationCreatorId);
    }

    @Quando("esse usuário tenta aprovar a solicitação")
    public void requesterAttemptsToApprove() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/reservations/{id}/approval", reservationId)
                        .with(profileToken("requester-2", "SOLICITANTE"))
                        .with(csrf()))
            .andExpect(status().is2xxSuccessful())
                .andReturn();
        currentResponse = readReservationResponse(result);
    }

    @Entao("a operação deve ser negada")
    public void operationMustBeDenied() {
        assertEquals(ReservationStatus.SOLICITADA.name(), currentResponse.status());
        assertNull(currentResponse.approvalDecision());
    }

    @Entao("o estado e a decisão da reserva devem permanecer inalterados")
    public void reservationStateAndDecisionMustRemainUnchanged() {
        assertEquals(ReservationStatus.SOLICITADA.name(), currentResponse.status());
        assertNull(currentResponse.approvalDecision());
    }

    @Dado("que um recurso possui período de manutenção registrado")
    public void resourceHasMaintenancePeriod() throws Exception {
        MaintenancePeriod period = new MaintenancePeriod(
                Instant.parse("2026-10-25T10:00:00Z"),
                Instant.parse("2026-10-25T11:00:00Z"));
        mockMvc.perform(post("/api/resources/{resourceId}/maintenance", "room-1")
                        .with(profileToken("admin-1", "ADMINISTRADOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(period)))
                        .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    @Dado("que o período solicitado para reserva contém tempo dentro da manutenção")
    public void requestedPeriodOverlapsMaintenance() {
        requestedPeriod = new CreateReservationRequest(
                "room-1",
                Instant.parse("2026-10-25T09:59:59Z"),
                Instant.parse("2026-10-25T10:00:01Z"));
    }

    @Quando("o Solicitante tenta reservar o recurso")
    public void requesterAttemptsToBookResource() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/reservations")
                        .with(profileToken("requester-1", "SOLICITANTE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestedPeriod)))
                    .andExpect(status().is2xxSuccessful())
                .andReturn();
        accepted = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("accepted").asBoolean();
    }

    @Entao("a reserva deve ser recusada")
    public void reservationMustBeRejected() {
        assertFalse(accepted);
    }

    @Entao("o recurso deve ser tratado como indisponível para esse período")
    public void resourceMustBeUnavailableForPeriod() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/resources/{resourceId}/availability", "room-1")
                        .queryParam("startsAt", requestedPeriod.startsAt().toString())
                        .queryParam("endsAt", requestedPeriod.endsAt().toString())
                        .with(profileToken("requester-1", "SOLICITANTE")))
                    .andExpect(status().is2xxSuccessful())
                .andReturn();
        available = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("available").asBoolean();
        assertFalse(available);
    }

    @Dado("que uma reserva restrita foi solicitada e aguarda decisão")
    public void restrictedReservationWasRequestedAndWaitsForDecision() throws Exception {
        createRestrictedReservation("requester-1", "SOLICITANTE");
    }

    @Quando("um Responsável distinto aprova, inicia e conclui a reserva pelas operações públicas")
    public void responsibleApprovesStartsAndCompletesReservation() throws Exception {
        mockMvc.perform(post("/api/reservations/{id}/approval", reservationId)
                        .with(profileToken("responsible-1", "RESPONSAVEL"))
                        .with(csrf()))
            .andExpect(status().is2xxSuccessful())
                .andReturn();
        mockMvc.perform(post("/api/reservations/{id}/start", reservationId)
                        .with(profileToken("responsible-1", "RESPONSAVEL"))
                        .with(csrf()))
            .andExpect(status().is2xxSuccessful())
                .andReturn();
        MvcResult result = mockMvc.perform(post("/api/reservations/{id}/complete", reservationId)
                        .with(profileToken("responsible-1", "RESPONSAVEL"))
                        .with(csrf()))
            .andExpect(status().is2xxSuccessful())
                .andReturn();
        currentResponse = readReservationResponse(result);
    }

    @Entao("os estados observados devem seguir {string}, {string} e {string}")
    public void observedStatesMustFollow(String approved, String inUse, String completed) {
        List<String> observed = currentResponse.transitions().stream()
            .map(transition -> transition.to() == ReservationStatus.CONCLUIDA
                ? "CONCLUÍDA"
                : transition.to().name())
                .toList();
        assertEquals(List.of(approved, inUse, completed), observed);
    }

    @Entao("a resposta final deve expor os três registros de transição com ator e instante")
    public void finalResponseMustExposeThreeAuditedTransitions() {
        assertEquals(3, currentResponse.transitions().size());
        currentResponse.transitions().forEach(transition -> {
            assertEquals("responsible-1", transition.actorId());
            assertNotNull(transition.changedAt());
        });
    }

    private void createRestrictedReservation(String subject, String role) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/reservations/restricted")
                        .with(profileToken(subject, role))
                        .with(csrf()))
            .andExpect(status().is2xxSuccessful())
                .andReturn();
        currentResponse = readReservationResponse(result);
        reservationId = currentResponse.id();
        reservationCreatorId = subject;
        reservationCreatorRole = role;
    }

    private ReservationResponse readReservationResponse(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), ReservationResponse.class);
    }

    private RequestPostProcessor profileToken(String subject, String role) {
        return jwt().jwt(token -> token.subject(subject))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
