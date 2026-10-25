package br.com.organizacao.recursos.reservas;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReservationController {
    @GetMapping("/api/reservations/restricted")
    public Map<String, String> restrictedReservation() {
        return Map.of("status", ReservationStatus.APROVADA.name());
    }
}
