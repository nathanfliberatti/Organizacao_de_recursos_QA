package br.com.organizacao.recursos.reservas;

import java.time.Clock;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    private ReservationExtras extras;

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

    /** Opcional: ausente em testes de fatia que nao carregam o fluxo completo de reservas. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setExtras(ReservationExtras extras) {
        this.extras = extras;
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
        return ResponseEntity.status(HttpStatus.CREATED).body(response(id, loadReservation(id)));
    }

    @GetMapping("/api/reservations/{id}")
    public ReservationResponse findReservation(@PathVariable String id) {
        return response(id, loadReservation(id));
    }

    @GetMapping("/api/reservations/{id}/history")
    @PreAuthorize("hasAnyRole('RESPONSAVEL', 'ADMINISTRADOR')")
    public List<ReservationTransition> history(@PathVariable String id) {
        return reservationAuditService.history(id);
    }

    /**
     * Cancela a reserva (RF-04): transição auditada para CANCELADA, que libera o horário. O registro continua
     * consultável. Reservas iniciadas (EM_USO/CONCLUIDA) não podem ser canceladas nem apagadas (RF-15, 409).
     */
    @DeleteMapping("/api/reservations/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication authentication) {
        ReservationTransition transition = reservationAuditService.cancelReservation(
                id, authentication.getName(), canManageAny(authentication), clock, "Reserva cancelada");
        Reservation reservation = reservationCatalog.findById(id);
        if (reservation != null) {
            reservation.applyTransition(transition);
        }
        if (extras != null) {
            extras.onCancelled(id, authentication.getName());
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/reservations/{id}/approval")
    @PreAuthorize("hasRole('RESPONSAVEL')")
    public ReservationResponse approve(@PathVariable String id, Authentication authentication) {
        Reservation reservation = loadReservation(id);
        User actor = authenticatedUser(authentication);
        ReservationTransition transition = reservation.prepareApproval(
                actor, clock, "Reserva aprovada pelo Responsável");
        recordTransition(reservation, transition);
        return response(id, reservation);
    }

    /** Rejeição de uma solicitação pelo Responsável (RF-14); libera o horário. */
    @PostMapping("/api/reservations/{id}/rejection")
    @PreAuthorize("hasRole('RESPONSAVEL')")
    public ReservationResponse reject(@PathVariable String id, Authentication authentication) {
        return applyReleasingTransition(id, authentication, "rejeitada",
                (reservation, actor) -> reservation.prepareRejection(actor, clock, "Reserva rejeitada pelo Responsável"));
    }

    /** Registro de não comparecimento (RF-14) por Responsável ou Administrador; libera o horário. */
    @PostMapping("/api/reservations/{id}/no-show")
    @PreAuthorize("hasAnyRole('RESPONSAVEL', 'ADMINISTRADOR')")
    public ReservationResponse noShow(@PathVariable String id, Authentication authentication) {
        return applyReleasingTransition(id, authentication, "marcada como não compareceu",
                (reservation, actor) -> reservation.prepareNoShow(actor, clock, "Solicitante não compareceu"));
    }

    @PostMapping("/api/reservations")
    public ResponseEntity<ReservationAttemptResponse> create(
            @Valid @RequestBody CreateReservationRequest request,
            Authentication authentication) {
        ReservationAttemptResponse result = reservationSchedulingService.schedule(request, authentication.getName());
        if (result.accepted() && extras != null) {
            extras.onCreated(result.reservationId(), request.resourceId(), authentication.getName());
        }
        HttpStatus status = result.accepted() ? HttpStatus.CREATED : HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(result);
    }

    @PutMapping("/api/reservations/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> update(
            @PathVariable long id,
            @RequestBody UpdateReservationRequest request,
            Authentication authentication) {
        boolean canModifyAny = hasRole(authentication, "RESPONSAVEL")
                || hasRole(authentication, "ADMINISTRADOR");
        reservationSchedulingService.update(
                id,
                authentication.getName(),
                canModifyAny,
                new ReservationPeriod(request.startsAt(), request.endsAt()));
        if (extras != null) {
            extras.onUpdated(String.valueOf(id), authentication.getName());
        }
        return ResponseEntity.ok().build();
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
        reservationAuditService.assertCanOperate(id, authentication.getName(), canManageAny(authentication));
        return applyTransition(id, authentication,
                (reservation, actor) -> reservation.prepareStartUse(actor, clock, "Reserva iniciada"));
    }

    @PostMapping("/api/reservations/{id}/complete")
    public ReservationResponse complete(@PathVariable String id, Authentication authentication) {
        reservationAuditService.assertCanOperate(id, authentication.getName(), canManageAny(authentication));
        return applyTransition(id, authentication,
                (reservation, actor) -> reservation.prepareComplete(actor, clock, "Reserva concluída"));
    }

    private Reservation loadReservation(String id) {
        Reservation reservation = reservationCatalog.findById(id);
        if (reservation == null && extras != null) {
            reservation = extras.rehydrate(id);
        }
        if (reservation == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Reservation not found");
        }
        return reservation;
    }

    private boolean canManageAny(Authentication authentication) {
        return hasRole(authentication, "RESPONSAVEL") || hasRole(authentication, "ADMINISTRADOR");
    }

    private User authenticatedUser(Authentication authentication) {
        UserRole role = UserRole.SOLICITANTE;
        if (hasRole(authentication, "RESPONSAVEL")) {
            role = UserRole.RESPONSAVEL;
        } else if (hasRole(authentication, "ADMINISTRADOR")) {
            role = UserRole.ADMINISTRADOR;
        }
        return new User(authentication.getName(), role);
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_" + role));
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
        Reservation reservation = loadReservation(id);
        ReservationTransition transition = action.prepare(reservation, authenticatedUser(authentication));
        recordTransition(reservation, transition);
        return response(id, reservation);
    }

    private ReservationResponse applyReleasingTransition(
            String id, Authentication authentication, String outcome, ReservationAction action) {
        Reservation reservation = loadReservation(id);
        ReservationTransition transition = action.prepare(reservation, authenticatedUser(authentication));
        recordTransition(reservation, transition);
        if (transition != null && extras != null) {
            extras.onScheduleReleased(id, authentication.getName(), outcome);
        }
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
