package br.com.organizacao.recursos.reservas;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/** RF-11: integração externa de notificação, verificada contra uma API simulada com WireMock. */
class NotificationWebhookTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneOffset.UTC);

    private WireMockServer api;
    private InMemoryNotificationStore store;

    @BeforeEach
    void startApi() {
        api = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        api.start();
        store = new InMemoryNotificationStore();
    }

    @AfterEach
    void stopApi() {
        api.stop();
    }

    private NotificationService service(String url) {
        return new NotificationService(store, CLOCK, url);
    }

    @ParameterizedTest
    @EnumSource(NotificationEvent.class)
    void everyEventIsPostedAsJsonAndMarkedAsDeliveredByWebhook(NotificationEvent event) {
        api.stubFor(post(urlEqualTo("/hook")).willReturn(aResponse().withStatus(204)));

        Notification saved = service(api.baseUrl() + "/hook").notify(event, "42", "solicitante", "Mensagem do evento");

        assertEquals("WEBHOOK", saved.delivery());
        api.verify(1, postRequestedFor(urlEqualTo("/hook"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson("""
                        {"event":"%s","reservationId":"42","recipientId":"solicitante",
                         "message":"Mensagem do evento","createdAt":"2026-11-12T10:00:00Z"}
                        """.formatted(event.name()))));
        assertEquals("WEBHOOK", store.list("solicitante", 10).getFirst().delivery());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404, 500, 503})
    void errorResponseMarksTheDeliveryAsFailedWithoutThrowing(int httpStatus) {
        api.stubFor(post(urlEqualTo("/hook")).willReturn(aResponse().withStatus(httpStatus)));

        Notification saved = service(api.baseUrl() + "/hook")
                .notify(NotificationEvent.CRIACAO, "1", "solicitante", "Reserva criada");

        assertEquals("WEBHOOK_FALHOU", saved.delivery());
        assertEquals(1, store.list("solicitante", 10).size());
    }

    @Test
    void slowApiTimesOutInsteadOfBlockingTheReservation() {
        api.stubFor(post(urlEqualTo("/hook")).willReturn(aResponse().withStatus(200).withFixedDelay(6000)));

        Notification saved = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> service(api.baseUrl() + "/hook")
                .notify(NotificationEvent.ALTERACAO, "1", "solicitante", "Reserva alterada"));

        assertEquals("WEBHOOK_FALHOU", saved.delivery());
    }

    @Test
    void unreachableApiMarksTheDeliveryAsFailed() {
        String url = api.baseUrl() + "/hook";
        api.stop();

        Notification saved = service(url).notify(NotificationEvent.CANCELAMENTO, "1", "solicitante", "Cancelada");

        assertEquals("WEBHOOK_FALHOU", saved.delivery());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "ftp://example.com/hook", "file:///etc/passwd", "isto não é uma url"})
    void withoutAValidHttpWebhookTheDeliveryStaysSimulatedAndNothingIsSent(String url) {
        Notification saved = service(url).notify(NotificationEvent.CRIACAO, "1", "solicitante", "Reserva criada");

        assertEquals("SIMULADA", saved.delivery());
        assertEquals(0, api.getAllServeEvents().size());
    }
}
