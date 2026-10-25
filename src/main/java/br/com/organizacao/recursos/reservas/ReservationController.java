package br.com.organizacao.recursos.reservas;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReservationController {
    @GetMapping("/api/reservations/restricted")
    public ReservationResponse restrictedReservation() {
        Reservation reservation = Reservation.requestForRestrictedResource();
        return new ReservationResponse(reservation.status().name());
    }
}
