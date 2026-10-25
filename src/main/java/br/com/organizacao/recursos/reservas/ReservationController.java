package br.com.organizacao.recursos.reservas;

import java.time.Clock;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReservationController {
    private final ReservationCatalog reservationCatalog;
    private final Clock clock;

    public ReservationController(ReservationCatalog reservationCatalog, Clock clock) {
        this.reservationCatalog = reservationCatalog;
        this.clock = clock;
    }

    @GetMapping("/api/reservations/restricted")
    public ReservationResponse restrictedReservation() {
        Reservation reservation = Reservation.requestForRestrictedResource();
        return new ReservationResponse(reservation.status().name(), reservation.approvalDecision());
    }

    @PostMapping("/api/reservations/{id}/approval")
    public ReservationResponse approve(@PathVariable String id, Authentication authentication) {
        Reservation reservation = reservationCatalog.findById(id);
        boolean responsible = authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_RESPONSAVEL"));
        UserRole role = responsible ? UserRole.RESPONSAVEL : UserRole.SOLICITANTE;
        reservation.approve(new User(authentication.getName(), role), clock);
        return new ReservationResponse(reservation.status().name(), reservation.approvalDecision());
    }
}
