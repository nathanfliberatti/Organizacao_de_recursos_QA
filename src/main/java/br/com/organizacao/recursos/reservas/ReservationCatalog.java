package br.com.organizacao.recursos.reservas;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ReservationCatalog {
    private final Map<String, Reservation> reservations = new ConcurrentHashMap<>();
    private final Map<String, ScheduledReservation> scheduledReservations = new ConcurrentHashMap<>();

    public String createRestrictedReservation(String requesterId) {
        String id = UUID.randomUUID().toString();
        createRestrictedReservation(id, requesterId);
        return id;
    }

    public void createRestrictedReservation(String id, String requesterId) {
        reservations.put(id, Reservation.requestForRestrictedResource(id, requesterId));
    }

    public Reservation findById(String id) {
        return reservations.get(id);
    }

    public void remove(String id) {
        reservations.remove(id);
    }

    public int count() {
        return reservations.size() + scheduledReservations.size();
    }

    public void clear() {
        reservations.clear();
        scheduledReservations.clear();
    }

    public void schedule(String resourceId, String requesterId, ReservationPeriod period) {
        String id = UUID.randomUUID().toString();
        scheduledReservations.put(id, new ScheduledReservation(resourceId, requesterId, period));
    }

    private record ScheduledReservation(String resourceId, String requesterId, ReservationPeriod period) {
    }
}
