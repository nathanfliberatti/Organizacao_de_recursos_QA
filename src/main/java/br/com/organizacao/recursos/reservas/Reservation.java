package br.com.organizacao.recursos.reservas;

import java.time.Clock;

public final class Reservation {
    private final ReservationStatus currentStatus;
    private ApprovalDecision approvalDecision;

    private Reservation(ReservationStatus currentStatus) {
        this.currentStatus = currentStatus;
    }

    public static Reservation requestForRestrictedResource() {
        return new Reservation(ReservationStatus.SOLICITADA);
    }

    public ReservationStatus status() {
        return currentStatus;
    }

    public void approve(User approver, Clock clock) {
    }

    public ApprovalDecision approvalDecision() {
        return approvalDecision;
    }
}
