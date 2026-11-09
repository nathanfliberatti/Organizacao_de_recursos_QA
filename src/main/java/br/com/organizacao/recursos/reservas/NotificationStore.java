package br.com.organizacao.recursos.reservas;

import java.util.List;

public interface NotificationStore {
    Notification save(Notification notification);

    /** Notificacoes mais recentes primeiro; {@code recipientId} nulo retorna todas. */
    List<Notification> list(String recipientId, int limit);
}
