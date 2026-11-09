package br.com.organizacao.recursos.reservas;

import java.time.Instant;

public record Notification(
        Long id,
        NotificationEvent event,
        String reservationId,
        String recipientId,
        String message,
        String delivery,
        Instant createdAt) {
}
