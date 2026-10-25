package br.com.organizacao.recursos.reservas;

import java.time.Clock;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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

    @PostMapping("/api/reservations/restricted")
    public ReservationResponse createRestrictedReservation(Authentication authentication) {
        String id = reservationCatalog.createRestrictedReservation(authentication.getName());
        return response(id, reservationCatalog.findById(id));
    }

    @GetMapping("/api/reservations/{id}")
    public ReservationResponse findReservation(@PathVariable String id) {
        return response(id, reservationCatalog.findById(id));
    }

    @PostMapping("/api/reservations/{id}/approval")
    public ReservationResponse approve(@PathVariable String id, Authentication authentication) {
        Reservation reservation = reservationCatalog.findById(id);
        reservation.approve(authenticatedUser(authentication), clock);
        return response(id, reservation);
    }

    @PostMapping("/api/reservations")
    public ReservationAttemptResponse create(
            @RequestBody CreateReservationRequest request,
            Authentication authentication) {
        boolean accepted = reservationSchedulingService.schedule(request, authentication.getName());
        return new ReservationAttemptResponse(accepted);
    }

    @GetMapping("/api/resources/{resourceId}/availability")
    public ResourceAvailabilityResponse availability(
            @PathVariable String resourceId,
            @RequestParam java.time.Instant startsAt,
            @RequestParam java.time.Instant endsAt) {
        ReservationPeriod period = new ReservationPeriod(startsAt, endsAt);
        return new ResourceAvailabilityResponse(reservationSchedulingService.isAvailable(resourceId, period));
    }

    @PostMapping("/api/reservations/{id}/start")
    public ReservationResponse start(@PathVariable String id, Authentication authentication) {
        return applyTransition(id, authentication, Reservation::startUse);
    }

    @PostMapping("/api/reservations/{id}/complete")
    public ReservationResponse complete(@PathVariable String id, Authentication authentication) {
        return applyTransition(id, authentication, Reservation::complete);
    }

    private User authenticatedUser(Authentication authentication) {
        boolean responsible = authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_RESPONSAVEL"));
        UserRole role = responsible ? UserRole.RESPONSAVEL : UserRole.SOLICITANTE;
        return new User(authentication.getName(), role);
    }

    private ReservationResponse response(Reservation reservation) {
        return new ReservationResponse(null, reservation.status().name(), reservation.approvalDecision(),
                reservation.transitions());
    }

    private ReservationResponse response(String id, Reservation reservation) {
        return new ReservationResponse(id, reservation.status().name(), reservation.approvalDecision(),
                reservation.transitions());
    }

    private ReservationResponse applyTransition(
            String id,
            Authentication authentication,
            ReservationAction action) {
        Reservation reservation = reservationCatalog.findById(id);
        action.apply(reservation, authenticatedUser(authentication), clock);
        return response(id, reservation);
    }

    @FunctionalInterface
    private interface ReservationAction {
        void apply(Reservation reservation, User actor, Clock clock);
    }
}
