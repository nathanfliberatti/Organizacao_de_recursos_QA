package br.com.organizacao.recursos.reservas;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReservationController {
    @GetMapping("/api/reservations/restricted")
    public Map<String, String> restrictedReservation() {
        Reservation reservation = Reservation.requestForRestrictedResource();
        return Map.of("status", reservation.status().name());
    }
}
