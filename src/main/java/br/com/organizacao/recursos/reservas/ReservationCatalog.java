package br.com.organizacao.recursos.reservas;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReservationCatalog {
    private final Map<String, Reservation> reservations = new ConcurrentHashMap<>();

    public String createRestrictedReservation(String requesterId) {
        String id = UUID.randomUUID().toString();
        reservations.put(id, Reservation.requestForRestrictedResource(requesterId));
        return id;
    }

    public Reservation findById(String id) {
        return reservations.get(id);
    }

    public int count() {
        return reservations.size();
    }
}
