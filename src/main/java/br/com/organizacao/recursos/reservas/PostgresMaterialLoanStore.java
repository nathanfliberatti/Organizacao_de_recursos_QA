package br.com.organizacao.recursos.reservas;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Repository
@Profile("postgres")
public class PostgresMaterialLoanStore implements MaterialLoanStore {
    private static final String COLUMNS = "loan_id, resource_id, borrower_id, registered_by, withdrawn_at,"
            + " returned_at, returned_registered_by, notes";
    private static final RowMapper<MaterialLoan> MAPPER = (resultSet, rowNumber) -> {
        Timestamp returnedAt = resultSet.getTimestamp("returned_at");
        return new MaterialLoan(
                resultSet.getLong("loan_id"),
                resultSet.getString("resource_id"),
                resultSet.getString("borrower_id"),
                resultSet.getString("registered_by"),
                resultSet.getTimestamp("withdrawn_at").toInstant(),
                returnedAt == null ? null : returnedAt.toInstant(),
                resultSet.getString("returned_registered_by"),
                resultSet.getString("notes"));
    };

    private final JdbcTemplate jdbcTemplate;

    public PostgresMaterialLoanStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public MaterialLoan withdraw(MaterialLoan loan) {
        try {
            Long id = jdbcTemplate.queryForObject("""
                    INSERT INTO material_loans (resource_id, borrower_id, registered_by, withdrawn_at, notes)
                    VALUES (?, ?, ?, ?, ?) RETURNING loan_id
                    """,
                    Long.class,
                    loan.resourceId(), loan.borrowerId(), loan.registeredBy(),
                    Timestamp.from(loan.withdrawnAt()), loan.notes());
            return new MaterialLoan(Objects.requireNonNull(id), loan.resourceId(), loan.borrowerId(),
                    loan.registeredBy(), loan.withdrawnAt(), null, null, loan.notes());
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Material is already withdrawn", exception);
        }
    }

    @Override
    @Transactional
    public MaterialLoan giveBack(long loanId, String registeredBy, Instant returnedAt) {
        int updated = jdbcTemplate.update("""
                UPDATE material_loans SET returned_at = ?, returned_registered_by = ?
                WHERE loan_id = ? AND returned_at IS NULL AND withdrawn_at <= ?
                """,
                Timestamp.from(returnedAt), registeredBy, loanId, Timestamp.from(returnedAt));
        if (updated != 1) {
            MaterialLoan existing = findById(loanId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));
            throw new ResponseStatusException(HttpStatus.CONFLICT, existing.open()
                    ? "Return time is before the withdrawal" : "Material was already returned");
        }
        return findById(loanId).orElseThrow();
    }

    @Override
    public Optional<MaterialLoan> findById(long loanId) {
        return jdbcTemplate.query("SELECT " + COLUMNS + " FROM material_loans WHERE loan_id = ?", MAPPER, loanId)
                .stream().findFirst();
    }

    @Override
    public List<MaterialLoan> list(String resourceId, Boolean open) {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM material_loans WHERE TRUE");
        List<Object> arguments = new ArrayList<>();
        if (resourceId != null) {
            sql.append(" AND resource_id = ?");
            arguments.add(resourceId);
        }
        if (open != null) {
            sql.append(open ? " AND returned_at IS NULL" : " AND returned_at IS NOT NULL");
        }
        sql.append(" ORDER BY loan_id DESC");
        return jdbcTemplate.query(sql.toString(), MAPPER, arguments.toArray());
    }
}
