package br.com.organizacao.recursos.reservas;

public final class Reservation {
    private final ReservationStatus currentStatus;

    private Reservation(ReservationStatus currentStatus) {
        this.currentStatus = currentStatus;
    }

    public static Reservation requestForRestrictedResource() {
        return new Reservation(ReservationStatus.SOLICITADA);
    }

    public ReservationStatus status() {
        return currentStatus;
    }
}
