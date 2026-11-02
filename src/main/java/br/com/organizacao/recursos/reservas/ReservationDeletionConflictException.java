package br.com.organizacao.recursos.reservas;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ReservationDeletionConflictException extends RuntimeException {
    public ReservationDeletionConflictException() {
        super("A reservation that has started cannot be deleted");
    }
}
