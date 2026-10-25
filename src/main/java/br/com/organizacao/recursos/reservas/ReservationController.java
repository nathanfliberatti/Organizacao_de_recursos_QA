package br.com.organizacao.recursos.reservas;

import java.time.Clock;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReservationController {
    private final ReservationCatalog reservationCatalog;
    private final MaintenanceCatalog maintenanceCatalog;
    private final Clock clock;

    public ReservationController(
            ReservationCatalog reservationCatalog,
            MaintenanceCatalog maintenanceCatalog,
            Clock clock) {
        this.reservationCatalog = reservationCatalog;
        this.maintenanceCatalog = maintenanceCatalog;
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
        reservation.approve(authenticatedUser(authentication), clock);
        return new ReservationResponse(reservation.status().name(), reservation.approvalDecision());
    }

    @PostMapping("/api/reservations")
    public ReservationAttemptResponse create(
            @RequestBody CreateReservationRequest request,
            Authentication authentication) {
        MaintenancePeriod maintenance = maintenanceCatalog.findByResourceId(request.resourceId());
        ReservationPeriod period = new ReservationPeriod(request.startsAt(), request.endsAt());
        if (maintenance != null && !new MaintenanceRule(clock).isReservable(period, maintenance)) {
            return new ReservationAttemptResponse(false);
        }

        reservationCatalog.schedule(request.resourceId(), authentication.getName(), period);
        return new ReservationAttemptResponse(true);
    }

    private User authenticatedUser(Authentication authentication) {
        boolean responsible = authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_RESPONSAVEL"));
        UserRole role = responsible ? UserRole.RESPONSAVEL : UserRole.SOLICITANTE;
        return new User(authentication.getName(), role);
    }
}
