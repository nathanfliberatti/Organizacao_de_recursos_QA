package br.com.organizacao.recursos.reservas;

import java.time.Clock;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final ReservationAuditService reservationAuditService;
    private final Clock clock;

    public ReservationController(
            ReservationCatalog reservationCatalog,
            ReservationSchedulingService reservationSchedulingService,
            ReservationAuditService reservationAuditService,
            Clock clock) {
        this.reservationCatalog = reservationCatalog;
        this.reservationSchedulingService = reservationSchedulingService;
        this.reservationAuditService = reservationAuditService;
        this.clock = clock;
    }

    @GetMapping("/api/reservations/restricted")
    public ReservationResponse restrictedReservation() {
        Reservation reservation = Reservation.requestForRestrictedResource();
        return response(reservation);
    }

    @PostMapping("/api/reservations/restricted")
    public ResponseEntity<ReservationResponse> createRestrictedReservation(Authentication authentication) {
        String id = java.util.UUID.randomUUID().toString();
        reservationAuditService.createReservation(
                id, authentication.getName(), clock, "Solicitação criada");
        reservationCatalog.createRestrictedReservation(id, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response(id, reservationCatalog.findById(id)));
    }

    @GetMapping("/api/reservations/{id}")
    public ReservationResponse findReservation(@PathVariable String id) {
        return response(id, reservationCatalog.findById(id));
    }

    @GetMapping("/api/reservations/{id}/history")
    @PreAuthorize("hasAnyRole('RESPONSAVEL', 'ADMINISTRADOR')")
    public List<ReservationTransition> history(@PathVariable String id) {
        return reservationAuditService.history(id);
    }

    @DeleteMapping("/api/reservations/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        reservationAuditService.deleteReservation(id);
        reservationCatalog.remove(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/reservations/{id}/approval")
    @PreAuthorize("hasRole('RESPONSAVEL')")
    public ReservationResponse approve(@PathVariable String id, Authentication authentication) {
        Reservation reservation = reservationCatalog.findById(id);
        User actor = authenticatedUser(authentication);
        ReservationTransition transition = reservation.prepareApproval(
                actor, clock, "Reserva aprovada pelo Responsável");
        recordTransition(reservation, transition);
        return response(id, reservation);
    }

    @PostMapping("/api/reservations")
    public ResponseEntity<ReservationAttemptResponse> create(
            @RequestBody CreateReservationRequest request,
            Authentication authentication) {
        boolean accepted = reservationSchedulingService.schedule(request, authentication.getName());
        HttpStatus status = accepted ? HttpStatus.CREATED : HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(new ReservationAttemptResponse(accepted));
    }

    @GetMapping("/api/resources/{resourceId}/availability")
    public ResourceAvailabilityResponse availability(
            @PathVariable String resourceId,
            @RequestParam(required = false) String professorId,
            @RequestParam java.time.Instant startsAt,
            @RequestParam java.time.Instant endsAt) {
        ReservationPeriod period = new ReservationPeriod(startsAt, endsAt);
        return new ResourceAvailabilityResponse(
                reservationSchedulingService.isAvailable(resourceId, professorId, period));
    }

    @PostMapping("/api/reservations/{id}/start")
    public ReservationResponse start(@PathVariable String id, Authentication authentication) {
        return applyTransition(id, authentication,
                (reservation, actor) -> reservation.prepareStartUse(actor, clock, "Reserva iniciada"));
    }

    @PostMapping("/api/reservations/{id}/complete")
    public ReservationResponse complete(@PathVariable String id, Authentication authentication) {
        return applyTransition(id, authentication,
                (reservation, actor) -> reservation.prepareComplete(actor, clock, "Reserva concluída"));
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
        ReservationTransition transition = action.prepare(reservation, authenticatedUser(authentication));
        recordTransition(reservation, transition);
        return response(id, reservation);
    }

    private void recordTransition(Reservation reservation, ReservationTransition transition) {
        if (transition == null) {
            return;
        }
        reservationAuditService.recordTransition(transition);
        reservation.applyTransition(transition);
    }

    @FunctionalInterface
    private interface ReservationAction {
        ReservationTransition prepare(Reservation reservation, User actor);
    }
}
