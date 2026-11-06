package br.com.organizacao.recursos.reservas;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidReservationPeriodException extends RuntimeException {
    public InvalidReservationPeriodException() {
        super("endsAt must be later than startsAt");
    }
}
