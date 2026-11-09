package br.com.organizacao.recursos.reservas;

import java.time.Clock;
import org.springframework.stereotype.Service;

/**
 * Notificacoes de reserva (RF-11). A entrega e simulada: o evento fica registrado e consultavel
 * em {@code GET /api/notifications}, pronto para trocar o canal por uma API externa.
 */
@Service
public class NotificationService {
    static final String SIMULATED = "SIMULADA";

    private final NotificationStore store;
    private final Clock clock;

    public NotificationService(NotificationStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    public Notification notify(NotificationEvent event, String reservationId, String recipientId, String message) {
        return store.save(new Notification(null, event, reservationId, recipientId, message, SIMULATED,
                clock.instant()));
    }
}
