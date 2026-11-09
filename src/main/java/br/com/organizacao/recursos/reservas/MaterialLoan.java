package br.com.organizacao.recursos.reservas;

import java.time.Instant;

public record MaterialLoan(
        Long id,
        String resourceId,
        String borrowerId,
        String registeredBy,
        Instant withdrawnAt,
        Instant returnedAt,
        String returnedRegisteredBy,
        String notes) {
    public boolean open() {
        return returnedAt == null;
    }
}
