package br.com.organizacao.recursos.reservas;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** RF-11 ponta a ponta: eventos de reserva chegam à API externa (WireMock) e a falha dela não derruba a reserva. */
@SpringBootTest(properties = "app.demo-data.enabled=true")
@AutoConfigureMockMvc
class NotificationWebhookApiTest {
    private static final String CREDENTIAL = UUID.randomUUID().toString();
    private static final WireMockServer EXTERNAL_API = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    static {
        EXTERNAL_API.start();
    }

    @DynamicPropertySource
    static void webhookProperties(DynamicPropertyRegistry registry) {
        registry.add("app.notifications.webhook-url", () -> EXTERNAL_API.baseUrl() + "/notificacoes");
        registry.add("app.demo-data.password", () -> CREDENTIAL);
    }

    @AfterAll
    static void stopExternalApi() {
        EXTERNAL_API.stop();
    }

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void resetExternalApi() {
        EXTERNAL_API.resetAll();
    }

    private String login(String username) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + CREDENTIAL + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.access_token");
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    private String newResource(String admin) throws Exception {
        String id = "wm-" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(as(post("/api/resources").content("{\"id\":\"" + id + "\",\"capacity\":10}"), admin))
                .andExpect(status().isCreated());
        return id;
    }

    private String reservationBody(String resource, String day) {
        return "{\"resourceId\":\"" + resource + "\",\"startsAt\":\"" + day + "T10:00:00Z\",\"endsAt\":\"" + day
                + "T11:00:00Z\"}";
    }

    @Test
    void creationAndCancellationReachTheExternalApi() throws Exception {
        EXTERNAL_API.stubFor(WireMock.post(urlEqualToHook()).willReturn(aResponse().withStatus(200)));
        String admin = login("admin");
        String solicitante = login("solicitante");
        String resource = newResource(admin);

        String created = mockMvc.perform(as(post("/api/reservations").content(reservationBody(resource, "2029-05-01")),
                        solicitante)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = String.valueOf(((Number) JsonPath.read(created, "$.reservationId")).longValue());
        mockMvc.perform(as(delete("/api/reservations/" + id), solicitante)).andExpect(status().isNoContent());

        EXTERNAL_API.verify(1, postRequestedFor(urlEqualToHook())
                .withRequestBody(matchingJsonPath("$.event", WireMock.equalTo("CRIACAO")))
                .withRequestBody(matchingJsonPath("$.reservationId", WireMock.equalTo(id)))
                .withRequestBody(matchingJsonPath("$.recipientId", WireMock.equalTo("solicitante"))));
        EXTERNAL_API.verify(1, postRequestedFor(urlEqualToHook())
                .withRequestBody(matchingJsonPath("$.event", WireMock.equalTo("CANCELAMENTO"))));
        mockMvc.perform(as(get("/api/notifications"), solicitante))
                .andExpect(jsonPath("$[?(@.reservationId=='" + id + "')].delivery").value(
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("WEBHOOK"))));
    }

    @Test
    void failureOfTheExternalApiNeverBlocksTheReservation() throws Exception {
        EXTERNAL_API.stubFor(WireMock.post(urlEqualToHook()).willReturn(aResponse().withStatus(500)));
        String admin = login("admin");
        String solicitante = login("solicitante");
        String resource = newResource(admin);

        String created = mockMvc.perform(as(post("/api/reservations").content(reservationBody(resource, "2029-05-02")),
                        solicitante)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = String.valueOf(((Number) JsonPath.read(created, "$.reservationId")).longValue());

        mockMvc.perform(as(get("/api/reservations/" + id), solicitante)).andExpect(status().isOk());
        mockMvc.perform(as(get("/api/notifications"), solicitante))
                .andExpect(jsonPath("$[?(@.reservationId=='" + id + "' && @.event=='CRIACAO')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.reservationId=='" + id + "')].delivery").value("WEBHOOK_FALHOU"));
    }

    private static com.github.tomakehurst.wiremock.matching.UrlPattern urlEqualToHook() {
        return urlEqualTo("/notificacoes");
    }
}
