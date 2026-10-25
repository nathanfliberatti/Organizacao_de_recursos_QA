package br.com.organizacao.recursos.reservas;

import java.time.Clock;

public final class Reservation {
    private final String requesterId;
    private ReservationStatus currentStatus;
    private ApprovalDecision approvalDecision;

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
        currentStatus = ReservationStatus.APROVADA;
    }

    public ApprovalDecision approvalDecision() {
        return approvalDecision;
    }
}
