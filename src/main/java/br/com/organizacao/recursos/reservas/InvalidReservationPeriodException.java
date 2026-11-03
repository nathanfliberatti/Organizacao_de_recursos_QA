package br.com.organizacao.recursos.reservas;

public class InvalidReservationPeriodException extends RuntimeException {
    public InvalidReservationPeriodException() {
        super("endsAt must be later than startsAt");
    }
}
