package br.com.organizacao.recursos.reservas;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Fluxo completo da aplicacao com login real (JWT): recursos, reservas, ciclo de vida,
 * manutencao, materiais e notificacoes. Executado com armazenamento em memoria e com PostgreSQL.
 */
abstract class AbstractApplicationFlowTest {
    protected static final String PASSWORD = UUID.randomUUID().toString();

    @DynamicPropertySource
    static void demoUserCredentials(DynamicPropertyRegistry registry) {
        registry.add("app.demo-data.password", () -> PASSWORD);
    }

    @Autowired
    protected MockMvc mockMvc;

    protected String login(String username) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.access_token");
    }

    protected MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    protected String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void loginRejectsWrongPasswordAndProtectedEndpointsRequireToken() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"errada\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/resources")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    void roleIsReturnedByLoginAndMe() throws Exception {
        String token = login("responsavel");
        mockMvc.perform(as(get("/api/auth/me"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("RESPONSAVEL"));
    }

    @Test
    void administratorRegistersAndSearchesResources() throws Exception {
        String admin = login("admin");
        String solicitante = login("solicitante");
        String id = "sala-" + suffix();
        String place = "Bloco " + suffix();
        String body = "{\"id\":\"" + id + "\",\"name\":\"Sala Teste\",\"type\":\"SALA\",\"capacity\":30,"
                + "\"location\":\"" + place + "\",\"competency\":null,\"restricted\":false}";

        mockMvc.perform(as(post("/api/resources").content(body), solicitante)).andExpect(status().isForbidden());
        mockMvc.perform(as(post("/api/resources").content(body), admin)).andExpect(status().isCreated());
        mockMvc.perform(as(post("/api/resources").content(body), admin)).andExpect(status().isConflict());

        mockMvc.perform(as(get("/api/resources?type=SALA&minCapacity=30&location=" + place.toLowerCase() + ""), solicitante))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id + "')]", hasSize(1)));
        mockMvc.perform(as(get("/api/resources?type=MATERIAL&location=" + place.toLowerCase() + ""), solicitante))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(as(get("/api/resources?minCapacity=31&location=" + place.toLowerCase() + ""), solicitante))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(as(get("/api/resources/" + id), solicitante))
                .andExpect(jsonPath("$.name").value("Sala Teste"));

        mockMvc.perform(as(put("/api/resources/" + id).content(body.replace("\"capacity\":30", "\"capacity\":50")), admin))
                .andExpect(status().isOk());
        mockMvc.perform(as(get("/api/resources/" + id), solicitante)).andExpect(jsonPath("$.capacity").value(50));
        mockMvc.perform(as(get("/api/resources/inexistente-" + suffix()), solicitante)).andExpect(status().isNotFound());
    }

    @Test
    void searchesResourcesByCompetencyAndFreeText() throws Exception {
        String admin = login("admin");
        String solicitante = login("solicitante");
        String tag = suffix();
        String id = "prof-" + tag;
        mockMvc.perform(as(post("/api/resources").content("{\"id\":\"" + id + "\",\"name\":\"Prof Busca " + tag
                + "\",\"type\":\"PROFESSOR\",\"capacity\":1,\"competency\":\"Calculo Diferencial " + tag
                + "\"}"), admin)).andExpect(status().isCreated());

        mockMvc.perform(as(get("/api/resources?type=PROFESSOR&competency=diferencial " + tag), solicitante))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(id));
        mockMvc.perform(as(get("/api/resources?competency=algebra " + tag), solicitante))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(as(get("/api/resources?text=busca " + tag), solicitante))
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(as(get("/api/resources?text=" + id), solicitante))
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(as(get("/api/resources?text=inexistente-" + tag), solicitante))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void reservationLifecycleFollowsResourceRestrictionAndNotifiesEvents() throws Exception {
        String admin = login("admin");
        String responsavel = login("responsavel");
        String solicitante = login("solicitante");
        String open = "livre-" + suffix();
        String restricted = "restrita-" + suffix();
        createResource(admin, open, false);
        createResource(admin, restricted, true);

        long openId = createReservation(solicitante, open, "2027-03-01T10:00:00Z", "2027-03-01T11:00:00Z");
        long restrictedId = createReservation(solicitante, restricted, "2027-03-01T12:00:00Z", "2027-03-01T13:00:00Z");

        mockMvc.perform(as(get("/api/reservations/" + openId), solicitante))
                .andExpect(jsonPath("$.status").value("APROVADA"));
        mockMvc.perform(as(get("/api/reservations/" + restrictedId), solicitante))
                .andExpect(jsonPath("$.status").value("SOLICITADA"));

        // Somente o Responsavel aprova e nunca o proprio solicitante.
        mockMvc.perform(as(post("/api/reservations/" + restrictedId + "/approval"), solicitante))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(post("/api/reservations/" + restrictedId + "/approval"), responsavel))
                .andExpect(jsonPath("$.status").value("APROVADA"));
        mockMvc.perform(as(post("/api/reservations/" + restrictedId + "/start"), solicitante))
                .andExpect(jsonPath("$.status").value("EM_USO"));
        mockMvc.perform(as(delete("/api/reservations/" + restrictedId), solicitante))
                .andExpect(status().isConflict());
        mockMvc.perform(as(post("/api/reservations/" + restrictedId + "/complete"), solicitante))
                .andExpect(jsonPath("$.status").value("CONCLUIDA"));

        mockMvc.perform(as(get("/api/reservations/" + restrictedId + "/history"), responsavel))
                .andExpect(jsonPath("$", hasSize(4)));

        // Alteracao e cancelamento da reserva livre.
        mockMvc.perform(as(put("/api/reservations/" + openId)
                        .content("{\"startsAt\":\"2027-03-01T14:00:00Z\",\"endsAt\":\"2027-03-01T15:00:00Z\"}"), solicitante))
                .andExpect(status().isOk());
        mockMvc.perform(as(delete("/api/reservations/" + openId), solicitante)).andExpect(status().isNoContent());

        mockMvc.perform(as(get("/api/reservations"), solicitante))
                .andExpect(jsonPath("$[?(@.id=='" + openId + "')].schedulingStatus").value("CANCELADA"))
                .andExpect(jsonPath("$[?(@.id=='" + restrictedId + "')].lifecycleStatus").value("CONCLUIDA"));

        mockMvc.perform(as(get("/api/notifications"), solicitante))
                .andExpect(jsonPath("$[?(@.reservationId=='" + openId + "' && @.event=='CRIACAO')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.reservationId=='" + openId + "' && @.event=='ALTERACAO')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.reservationId=='" + openId + "' && @.event=='CANCELAMENTO')]", hasSize(1)));
    }

    @Test
    void anotherRequesterCannotCancelOrChangeReservation() throws Exception {
        String admin = login("admin");
        String solicitante = login("solicitante");
        String resource = "sala-" + suffix();
        createResource(admin, resource, false);
        long id = createReservation(solicitante, resource, "2027-04-01T10:00:00Z", "2027-04-01T11:00:00Z");
        String other = createUserAndLogin(admin, "outro-" + suffix());

        mockMvc.perform(as(delete("/api/reservations/" + id), other)).andExpect(status().isForbidden());
        mockMvc.perform(as(put("/api/reservations/" + id)
                        .content("{\"startsAt\":\"2027-04-01T12:00:00Z\",\"endsAt\":\"2027-04-01T13:00:00Z\"}"), other))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(get("/api/reservations"), other))
                .andExpect(jsonPath("$[?(@.id=='" + id + "')]", hasSize(0)));
    }

    @Test
    void maintenanceBlocksReservationsAndCanBeRemoved() throws Exception {
        String admin = login("admin");
        String solicitante = login("solicitante");
        String resource = "sala-" + suffix();
        createResource(admin, resource, false);
        String period = "{\"startsAt\":\"2027-05-01T08:00:00Z\",\"endsAt\":\"2027-05-01T18:00:00Z\"}";

        mockMvc.perform(as(post("/api/resources/" + resource + "/maintenance").content(period), solicitante))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(post("/api/resources/" + resource + "/maintenance").content(period), admin))
                .andExpect(status().isCreated());
        mockMvc.perform(as(get("/api/maintenance"), solicitante))
                .andExpect(jsonPath("$[?(@.resourceId=='" + resource + "')]", hasSize(1)));
        mockMvc.perform(as(post("/api/reservations").content("{\"resourceId\":\"" + resource
                        + "\",\"startsAt\":\"2027-05-01T09:00:00Z\",\"endsAt\":\"2027-05-01T10:00:00Z\"}"), solicitante))
                .andExpect(status().isConflict());
        mockMvc.perform(as(get("/api/resources?availableFrom=2027-05-01T09:00:00Z&availableTo=2027-05-01T10:00:00Z"
                        + "&text=" + resource), solicitante))
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(as(delete("/api/resources/" + resource + "/maintenance"), admin)).andExpect(status().isNoContent());
        createReservation(solicitante, resource, "2027-05-01T09:00:00Z", "2027-05-01T10:00:00Z");
    }

    @Test
    void materialWithdrawalAndReturnAreRecordedAndQueryable() throws Exception {
        String admin = login("admin");
        String responsavel = login("responsavel");
        String solicitante = login("solicitante");
        String material = "projetor-" + suffix();
        mockMvc.perform(as(post("/api/resources").content("{\"id\":\"" + material
                + "\",\"capacity\":1,\"type\":\"MATERIAL\"}"), admin)).andExpect(status().isCreated());
        String withdrawal = "{\"borrowerId\":\"solicitante\",\"notes\":\"aula de sexta\"}";

        mockMvc.perform(as(post("/api/materials/" + material + "/withdrawals").content(withdrawal), solicitante))
                .andExpect(status().isForbidden());
        String created = mockMvc.perform(as(post("/api/materials/" + material + "/withdrawals").content(withdrawal), responsavel))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int loanId = JsonPath.read(created, "$.id");
        mockMvc.perform(as(post("/api/materials/" + material + "/withdrawals").content(withdrawal), responsavel))
                .andExpect(status().isConflict());
        mockMvc.perform(as(get("/api/loans?open=true&resourceId=" + material), responsavel))
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(as(post("/api/loans/" + loanId + "/return"), responsavel))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnedRegisteredBy").value("responsavel"));
        mockMvc.perform(as(post("/api/loans/" + loanId + "/return"), responsavel)).andExpect(status().isConflict());
        mockMvc.perform(as(get("/api/loans/" + loanId), responsavel))
                .andExpect(jsonPath("$.returnedAt").isNotEmpty());
        mockMvc.perform(as(get("/api/loans?open=true&resourceId=" + material), responsavel))
                .andExpect(jsonPath("$", hasSize(0)));
        // Um material so pode ser retirado de novo depois da devolucao e recursos que nao sao material sao recusados.
        mockMvc.perform(as(post("/api/materials/" + material + "/withdrawals").content(withdrawal), responsavel))
                .andExpect(status().isCreated());
        String room = "sala-" + suffix();
        createResource(admin, room, false);
        mockMvc.perform(as(post("/api/materials/" + room + "/withdrawals").content(withdrawal), responsavel))
                .andExpect(status().isBadRequest());
    }

    protected void createResource(String adminToken, String id, boolean restricted) throws Exception {
        mockMvc.perform(as(post("/api/resources").content("{\"id\":\"" + id + "\",\"capacity\":20,\"type\":\"SALA\","
                        + "\"restricted\":" + restricted + "}"), adminToken))
                .andExpect(status().isCreated());
    }

    protected long createReservation(String token, String resourceId, String startsAt, String endsAt) throws Exception {
        String body = mockMvc.perform(as(post("/api/reservations").content("{\"resourceId\":\"" + resourceId
                        + "\",\"startsAt\":\"" + startsAt + "\",\"endsAt\":\"" + endsAt + "\"}"), token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.reservationId")).longValue();
    }

    protected String createUserAndLogin(String adminToken, String username) throws Exception {
        mockMvc.perform(as(post("/api/users").content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD
                        + "\",\"displayName\":\"Outro\",\"role\":\"SOLICITANTE\"}"), adminToken))
                .andExpect(status().isCreated());
        return login(username);
    }

    @Test
    void overlappingReservationForSameResourceIsRejectedWithConflict() throws Exception {
        String admin = login("admin");
        String solicitante = login("solicitante");
        String resource = "sala-" + suffix();
        createResource(admin, resource, false);
        createReservation(solicitante, resource, "2027-06-01T10:00:00Z", "2027-06-01T11:00:00Z");

        mockMvc.perform(as(post("/api/reservations").content("{\"resourceId\":\"" + resource
                        + "\",\"professorId\":\"outro-prof\",\"startsAt\":\"2027-06-01T10:30:00Z\","
                        + "\"endsAt\":\"2027-06-01T11:30:00Z\"}"), solicitante))
                .andExpect(status().isConflict());
        // Periodos que apenas encostam nao conflitam.
        createReservation(solicitante, resource, "2027-06-01T11:00:00Z", "2027-06-01T12:00:00Z");
    }

    @Test
    void concurrentRequestsForSameSlotConfirmOnlyOneReservation() throws Exception {
        String admin = login("admin");
        String solicitante = login("solicitante");
        String resource = "sala-" + suffix();
        createResource(admin, resource, false);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<CompletableFuture<Integer>> calls = java.util.stream.IntStream.range(0, 8)
                    .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                        try {
                            return mockMvc.perform(as(post("/api/reservations").content("{\"resourceId\":\"" + resource
                                            + "\",\"professorId\":\"prof-" + i + "\",\"startsAt\":\"2027-07-01T10:00:00Z\","
                                            + "\"endsAt\":\"2027-07-01T11:00:00Z\"}"), solicitante))
                                    .andReturn().getResponse().getStatus();
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    }, executor))
                    .toList();
            long created = calls.stream().map(CompletableFuture::join).filter(code -> code == 201).count();
            assertEquals(1, created);
        } finally {
            executor.shutdownNow();
        }
    }
}
