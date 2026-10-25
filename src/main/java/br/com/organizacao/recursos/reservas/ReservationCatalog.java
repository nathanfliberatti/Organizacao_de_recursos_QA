package br.com.organizacao.recursos.reservas;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReservationCatalog {
    private final Map<String, Reservation> reservations = new ConcurrentHashMap<>();
    private final Map<String, ScheduledReservation> scheduledReservations = new ConcurrentHashMap<>();

    public String createRestrictedReservation(String requesterId) {
        String id = UUID.randomUUID().toString();
        reservations.put(id, Reservation.requestForRestrictedResource(requesterId));
        return id;
    }

    public Reservation findById(String id) {
        return reservations.get(id);
    }

    public int count() {
        return reservations.size() + scheduledReservations.size();
    }

    public void schedule(String resourceId, String requesterId, ReservationPeriod period) {
        String id = UUID.randomUUID().toString();
        scheduledReservations.put(id, new ScheduledReservation(resourceId, requesterId, period));
    }

    private record ScheduledReservation(String resourceId, String requesterId, ReservationPeriod period) {
    }
}
