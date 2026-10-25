package br.com.organizacao.recursos.reservas;

public final class Reservation {
    private final ReservationStatus status;

    private Reservation(ReservationStatus status) {
        this.status = status;
    }

    public static Reservation requestForRestrictedResource() {
        return new Reservation(ReservationStatus.SOLICITADA);
    }

    public ReservationStatus status() {
        return status;
    }
}
