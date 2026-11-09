package br.com.organizacao.recursos.reservas;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
public class InMemoryNotificationStore implements NotificationStore {
    private final List<Notification> notifications = new CopyOnWriteArrayList<>();
    private final AtomicLong ids = new AtomicLong();

    @Override
    public Notification save(Notification notification) {
        Notification saved = new Notification(ids.incrementAndGet(), notification.event(),
                notification.reservationId(), notification.recipientId(), notification.message(),
                notification.delivery(), notification.createdAt());
        notifications.add(saved);
        return saved;
    }

    @Override
    public List<Notification> list(String recipientId, int limit) {
        return notifications.stream()
                .filter(notification -> recipientId == null || recipientId.equals(notification.recipientId()))
                .sorted(Comparator.comparing(Notification::id).reversed())
                .limit(limit)
                .toList();
    }
}
