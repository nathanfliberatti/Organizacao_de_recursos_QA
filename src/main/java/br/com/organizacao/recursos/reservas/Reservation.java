package br.com.organizacao.recursos.reservas;

import java.time.Clock;
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
        if (requesterId.equals(approver.id())) {
            return;
        }
        approvalDecision = new ApprovalDecision(approver, clock.instant());
        transitionTo(ReservationStatus.APROVADA, approver, clock);
    }

    public ApprovalDecision approvalDecision() {
        return approvalDecision;
    }

    public void startUse(User actor, Clock clock) {
        transitionTo(ReservationStatus.EM_USO, actor, clock);
    }

    public void complete(User actor, Clock clock) {
        transitionTo(ReservationStatus.CONCLUIDA, actor, clock);
    }

    public List<ReservationTransition> transitions() {
        return List.copyOf(transitions);
    }

    private void transitionTo(ReservationStatus nextStatus, User actor, Clock clock) {
        if (!isAllowedTransitionTo(nextStatus)) {
            return;
        }

        transitions.add(new ReservationTransition(currentStatus, nextStatus, actor.id(), clock.instant()));
        currentStatus = nextStatus;
    }

    private boolean isAllowedTransitionTo(ReservationStatus nextStatus) {
        return switch (currentStatus) {
            case SOLICITADA -> nextStatus == ReservationStatus.APROVADA;
            case APROVADA -> nextStatus == ReservationStatus.EM_USO;
            case EM_USO -> nextStatus == ReservationStatus.CONCLUIDA;
            case CONCLUIDA -> false;
        };
    }
}
