package br.com.organizacao.recursos.reservas;

import java.time.Clock;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class ReservationController {
    private final ReservationCatalog reservationCatalog;
    private final ReservationSchedulingService reservationSchedulingService;
    private final Clock clock;

    public ReservationController(
            ReservationCatalog reservationCatalog,
            ReservationSchedulingService reservationSchedulingService,
            Clock clock) {
        this.reservationCatalog = reservationCatalog;
        this.reservationSchedulingService = reservationSchedulingService;
        this.clock = clock;
    }

    @GetMapping("/api/reservations/restricted")
    public ReservationResponse restrictedReservation() {
        Reservation reservation = Reservation.requestForRestrictedResource();
        return response(reservation);
    }

    @PostMapping("/api/reservations/{id}/approval")
    public ReservationResponse approve(@PathVariable String id, Authentication authentication) {
        Reservation reservation = reservationCatalog.findById(id);
        reservation.approve(authenticatedUser(authentication), clock);
        return response(reservation);
    }

    @PostMapping("/api/reservations")
    public ReservationAttemptResponse create(
            @RequestBody CreateReservationRequest request,
            Authentication authentication) {
        boolean accepted = reservationSchedulingService.schedule(request, authentication.getName());
        return new ReservationAttemptResponse(accepted);
    }

    @PostMapping("/api/reservations/{id}/start")
    public ReservationResponse start(@PathVariable String id, Authentication authentication) {
        return response(reservationCatalog.findById(id));
    }

    @PostMapping("/api/reservations/{id}/complete")
    public ReservationResponse complete(@PathVariable String id, Authentication authentication) {
        return response(reservationCatalog.findById(id));
    }

    private User authenticatedUser(Authentication authentication) {
        boolean responsible = authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_RESPONSAVEL"));
        UserRole role = responsible ? UserRole.RESPONSAVEL : UserRole.SOLICITANTE;
        return new User(authentication.getName(), role);
    }

    private ReservationResponse response(Reservation reservation) {
        return new ReservationResponse(reservation.status().name(), reservation.approvalDecision(),
                reservation.transitions());
    }
}
