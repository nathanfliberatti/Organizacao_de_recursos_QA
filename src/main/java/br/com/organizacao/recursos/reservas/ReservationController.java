package br.com.organizacao.recursos.reservas;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReservationController {
    private final ReservationCatalog reservationCatalog;

    public ReservationController(ReservationCatalog reservationCatalog) {
        this.reservationCatalog = reservationCatalog;
    }

    @GetMapping("/api/reservations/restricted")
    public ReservationResponse restrictedReservation() {
        Reservation reservation = Reservation.requestForRestrictedResource();
        return new ReservationResponse(reservation.status().name(), reservation.approvalDecision());
    }

    @PostMapping("/api/reservations/{id}/approval")
    public ReservationResponse approve(@PathVariable String id) {
        return new ReservationResponse(ReservationStatus.APROVADA.name(), null);
    }
}
