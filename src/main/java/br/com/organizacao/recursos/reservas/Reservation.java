package br.com.organizacao.recursos.reservas;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class Reservation {
    private final String requesterId;
    private ReservationStatus currentStatus;
    private ApprovalDecision approvalDecision;
    private final List<ReservationTransition> transitions = new ArrayList<>();

    private Reservation(String requesterId, ReservationStatus currentStatus) {
        this.requesterId = requesterId;
        this.currentStatus = currentStatus;
    }

    public static Reservation requestForRestrictedResource() {
        return requestForRestrictedResource("requester-1");
    }

    public static Reservation requestForRestrictedResource(String requesterId) {
        return new Reservation(requesterId, ReservationStatus.SOLICITADA);
    }

    public ReservationStatus status() {
        return currentStatus;
    }

    public void approve(User approver, Clock clock) {
        if (!isEligibleResponsible(approver) || !isAllowedTransitionTo(ReservationStatus.APROVADA)) {
            return;
        }
        Instant decidedAt = clock.instant();
        if (transitionTo(ReservationStatus.APROVADA, approver, decidedAt)) {
            approvalDecision = new ApprovalDecision(approver, decidedAt);
        }
    }

    public ApprovalDecision approvalDecision() {
        return approvalDecision;
    }

    public void startUse(User actor, Clock clock) {
        transitionTo(ReservationStatus.EM_USO, actor, clock.instant());
    }

    public void complete(User actor, Clock clock) {
        transitionTo(ReservationStatus.CONCLUIDA, actor, clock.instant());
    }

    public List<ReservationTransition> transitions() {
        return List.copyOf(transitions);
    }

    private boolean transitionTo(ReservationStatus nextStatus, User actor, Instant changedAt) {
        if (!isAllowedTransitionTo(nextStatus)) {
            return false;
        }

        transitions.add(new ReservationTransition(currentStatus, nextStatus, actor.id(), changedAt));
        currentStatus = nextStatus;
        return true;
    }

    private boolean isAllowedTransitionTo(ReservationStatus nextStatus) {
        return switch (currentStatus) {
            case SOLICITADA -> nextStatus == ReservationStatus.APROVADA;
            case APROVADA -> nextStatus == ReservationStatus.EM_USO;
            case EM_USO -> nextStatus == ReservationStatus.CONCLUIDA;
            case CONCLUIDA -> false;
        };
    }

    private boolean isEligibleResponsible(User approver) {
        return approver.role() == UserRole.RESPONSAVEL && !requesterId.equals(approver.id());
    }
}
