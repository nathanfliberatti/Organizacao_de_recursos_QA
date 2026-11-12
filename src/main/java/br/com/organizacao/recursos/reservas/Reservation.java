package br.com.organizacao.recursos.reservas;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Reservation {
    private final String id;
    private final String requesterId;
    private ReservationStatus currentStatus;
    private ApprovalDecision approvalDecision;
    private final List<ReservationTransition> transitions = new ArrayList<>();

    private Reservation(String id, String requesterId, ReservationStatus currentStatus) {
        this.id = id;
        this.requesterId = requesterId;
        this.currentStatus = currentStatus;
    }

    public static Reservation requestForRestrictedResource() {
        return requestForRestrictedResource("requester-1");
    }

    public static Reservation requestForRestrictedResource(String requesterId) {
        return requestForRestrictedResource(UUID.randomUUID().toString(), requesterId);
    }

    public static Reservation requestForRestrictedResource(String id, String requesterId) {
        return new Reservation(id, requesterId, ReservationStatus.SOLICITADA);
    }

    public String id() {
        return id;
    }

    public ReservationStatus status() {
        return currentStatus;
    }

    public void approve(User approver, Clock clock) {
        approve(approver, clock, "Reserva aprovada pelo Responsável");
    }

    public void approve(User approver, Clock clock, String reason) {
        ReservationTransition transition = prepareApproval(approver, clock, reason);
        if (transition != null) {
            applyTransition(transition);
        }
    }

    public ApprovalDecision approvalDecision() {
        return approvalDecision;
    }

    public void startUse(User actor, Clock clock) {
        startUse(actor, clock, "Reserva iniciada");
    }

    public void startUse(User actor, Clock clock, String reason) {
        applyIfAllowed(ReservationStatus.EM_USO, actor, clock, reason);
    }

    public void complete(User actor, Clock clock) {
        complete(actor, clock, "Reserva concluída");
    }

    public void complete(User actor, Clock clock, String reason) {
        applyIfAllowed(ReservationStatus.CONCLUIDA, actor, clock, reason);
    }

    public List<ReservationTransition> transitions() {
        return List.copyOf(transitions);
    }

    public boolean canBeDeleted() {
        return currentStatus != ReservationStatus.EM_USO && currentStatus != ReservationStatus.CONCLUIDA;
    }

    public ReservationTransition prepareApproval(User approver, Clock clock, String reason) {
        if (!isEligibleResponsible(approver)) {
            return null;
        }
        return prepareTransition(ReservationStatus.APROVADA, approver, clock.instant(), reason);
    }

    /** Rejeição: só o Responsável, e nunca o próprio solicitante (mesma regra da aprovação). */
    public ReservationTransition prepareRejection(User actor, Clock clock, String reason) {
        if (!isEligibleResponsible(actor)) {
            return null;
        }
        return prepareTransition(ReservationStatus.REJEITADA, actor, clock.instant(), reason);
    }

    /** Cancelamento: a posse (dono, Responsável ou Administrador) é verificada pelo serviço de auditoria. */
    public ReservationTransition prepareCancellation(User actor, Clock clock, String reason) {
        return prepareTransition(ReservationStatus.CANCELADA, actor, clock.instant(), reason);
    }

    /** Não comparecimento: só Responsável ou Administrador. */
    public ReservationTransition prepareNoShow(User actor, Clock clock, String reason) {
        if (actor.role() != UserRole.RESPONSAVEL && actor.role() != UserRole.ADMINISTRADOR) {
            return null;
        }
        return prepareTransition(ReservationStatus.NAO_COMPARECEU, actor, clock.instant(), reason);
    }

    public ReservationTransition prepareStartUse(User actor, Clock clock, String reason) {
        return prepareTransition(ReservationStatus.EM_USO, actor, clock.instant(), reason);
    }

    public ReservationTransition prepareComplete(User actor, Clock clock, String reason) {
        return prepareTransition(ReservationStatus.CONCLUIDA, actor, clock.instant(), reason);
    }

    public void applyTransition(ReservationTransition transition) {
        if (!id.equals(transition.reservationId())
                || currentStatus != transition.from()
                || !isAllowedTransitionTo(transition.to())) {
            throw new IllegalStateException("Reservation transition does not match the current state");
        }
        transitions.add(transition);
        currentStatus = transition.to();
        if (transition.to() == ReservationStatus.APROVADA) {
            approvalDecision = new ApprovalDecision(
                    new User(transition.actorId(), UserRole.RESPONSAVEL), transition.changedAt());
        }
    }

    private void applyIfAllowed(
            ReservationStatus nextStatus, User actor, Clock clock, String reason) {
        ReservationTransition transition = prepareTransition(nextStatus, actor, clock.instant(), reason);
        if (transition != null) {
            applyTransition(transition);
        }
    }

    private ReservationTransition prepareTransition(
            ReservationStatus nextStatus, User actor, Instant changedAt, String reason) {
        if (!isAllowedTransitionTo(nextStatus)) {
            return null;
        }
        return new ReservationTransition(id, currentStatus, nextStatus, actor.id(), changedAt, reason);
    }

    private boolean isAllowedTransitionTo(ReservationStatus nextStatus) {
        return currentStatus.canMoveTo(nextStatus);
    }

    private boolean isEligibleResponsible(User approver) {
        return approver.role() == UserRole.RESPONSAVEL && !requesterId.equals(approver.id());
    }
}
