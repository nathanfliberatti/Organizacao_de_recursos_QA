package br.com.organizacao.recursos.reservas;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
@Profile("!postgres")
public class InMemoryMaterialLoanStore implements MaterialLoanStore {
    private final ConcurrentMap<Long, MaterialLoan> loans = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();

    @Override
    public synchronized MaterialLoan withdraw(MaterialLoan loan) {
        boolean alreadyOut = loans.values().stream()
                .anyMatch(existing -> existing.resourceId().equals(loan.resourceId()) && existing.open());
        if (alreadyOut) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Material is already withdrawn");
        }
        MaterialLoan saved = new MaterialLoan(ids.incrementAndGet(), loan.resourceId(), loan.borrowerId(),
                loan.registeredBy(), loan.withdrawnAt(), null, null, loan.notes());
        loans.put(saved.id(), saved);
        return saved;
    }

    @Override
    public synchronized MaterialLoan giveBack(long loanId, String registeredBy, Instant returnedAt) {
        MaterialLoan loan = loans.get(loanId);
        if (loan == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found");
        }
        if (!loan.open()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Material was already returned");
        }
        MaterialLoan returned = new MaterialLoan(loan.id(), loan.resourceId(), loan.borrowerId(),
                loan.registeredBy(), loan.withdrawnAt(), returnedAt, registeredBy, loan.notes());
        loans.put(loanId, returned);
        return returned;
    }

    @Override
    public Optional<MaterialLoan> findById(long loanId) {
        return Optional.ofNullable(loans.get(loanId));
    }

    @Override
    public List<MaterialLoan> list(String resourceId, Boolean open) {
        return loans.values().stream()
                .filter(loan -> resourceId == null || resourceId.equals(loan.resourceId()))
                .filter(loan -> open == null || open == loan.open())
                .sorted(Comparator.comparing(MaterialLoan::id).reversed())
                .toList();
    }
}
