package br.com.organizacao.recursos.reservas;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Notificações de reserva (RF-11). Todo evento é gravado e consultável em {@code GET /api/notifications}.
 *
 * <p>Por padrão a entrega é <b>simulada</b> ({@code delivery = SIMULADA}). Se {@code app.notifications.webhook-url}
 * estiver definido, o evento também é enviado como {@code POST} JSON a essa URL (integração com API externa):
 * {@code delivery = WEBHOOK} quando a API responde 2xx e {@code WEBHOOK_FALHOU} caso contrário. Uma falha do
 * webhook (erro, lentidão, indisponibilidade) <b>nunca</b> impede nem desfaz a reserva. Não há reenvio.
 */
@Service
public class NotificationService {
    static final String SIMULATED = "SIMULADA";
    static final String WEBHOOK_SENT = "WEBHOOK";
    static final String WEBHOOK_FAILED = "WEBHOOK_FALHOU";
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);

    private final NotificationStore store;
    private final Clock clock;
    private final URI webhook;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public NotificationService(NotificationStore store, Clock clock) {
        this(store, clock, "");
    }

    @Autowired
    public NotificationService(
            NotificationStore store, Clock clock, @Value("${app.notifications.webhook-url:}") String webhookUrl) {
        this.store = store;
        this.clock = clock;
        this.webhook = parseWebhook(webhookUrl);
        this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    }

    public Notification notify(NotificationEvent event, String reservationId, String recipientId, String message) {
        Notification pending = new Notification(null, event, reservationId, recipientId, message,
                SIMULATED, clock.instant());
        if (webhook == null) {
            return store.save(pending);
        }
        String delivery = sendToWebhook(pending) ? WEBHOOK_SENT : WEBHOOK_FAILED;
        return store.save(new Notification(null, event, reservationId, recipientId, message, delivery,
                pending.createdAt()));
    }

    private boolean sendToWebhook(Notification notification) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("event", notification.event().name());
            body.put("reservationId", notification.reservationId());
            body.put("recipientId", notification.recipientId());
            body.put("message", notification.message());
            body.put("createdAt", notification.createdAt().toString());
            HttpRequest request = HttpRequest.newBuilder(webhook)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            boolean delivered = response.statusCode() / 100 == 2;
            if (!delivered) {
                LOGGER.warn("Webhook de notificação respondeu {} para o evento {}", response.statusCode(),
                        notification.event());
            }
            return delivered;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Envio do webhook de notificação interrompido");
            return false;
        } catch (Exception exception) {
            LOGGER.warn("Falha ao enviar o webhook de notificação do evento {}: {}", notification.event(),
                    exception.getClass().getSimpleName());
            return false;
        }
    }

    private static URI parseWebhook(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(url.trim());
            if ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) {
                return uri;
            }
        } catch (IllegalArgumentException exception) {
            // cai no aviso abaixo
        }
        LOGGER.warn("app.notifications.webhook-url ignorada: use uma URL http ou https válida");
        return null;
    }
}
