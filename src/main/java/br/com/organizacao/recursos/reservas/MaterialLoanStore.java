package br.com.organizacao.recursos.reservas;

import java.util.List;
import java.util.Optional;

public interface MaterialLoanStore {
    /** Registra a retirada; lanca 409 se o material ja estiver retirado. */
    MaterialLoan withdraw(MaterialLoan loan);

    /** Registra a devolucao; lanca 404 se a movimentacao nao existir e 409 se ja foi devolvida. */
    MaterialLoan giveBack(long loanId, String registeredBy, java.time.Instant returnedAt);

    Optional<MaterialLoan> findById(long loanId);

    List<MaterialLoan> list(String resourceId, Boolean open);
}
