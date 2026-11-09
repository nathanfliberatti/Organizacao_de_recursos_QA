package br.com.organizacao.recursos.reservas;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Clock;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Retirada e devolucao de materiais e equipamentos (RF-09). */
@RestController
public class MaterialLoanController {
    private final MaterialLoanStore loanStore;
    private final ResourceStore resourceStore;
    private final Clock clock;

    public MaterialLoanController(MaterialLoanStore loanStore, ResourceStore resourceStore, Clock clock) {
        this.loanStore = loanStore;
        this.resourceStore = resourceStore;
        this.clock = clock;
    }

    @PostMapping("/api/materials/{resourceId}/withdrawals")
    @PreAuthorize("hasAnyRole('RESPONSAVEL', 'ADMINISTRADOR')")
    public ResponseEntity<MaterialLoan> withdraw(
            @PathVariable String resourceId,
            @Valid @RequestBody WithdrawalRequest request,
            Authentication authentication) {
        Resource resource = resourceStore.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
        if (resource.type() != ResourceType.MATERIAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resource is not a material");
        }
        MaterialLoan loan = loanStore.withdraw(new MaterialLoan(null, resourceId, request.borrowerId().trim(),
                authentication.getName(), clock.instant(), null, null, request.notes()));
        return ResponseEntity.status(HttpStatus.CREATED).body(loan);
    }

    @PostMapping("/api/loans/{loanId}/return")
    @PreAuthorize("hasAnyRole('RESPONSAVEL', 'ADMINISTRADOR')")
    public MaterialLoan giveBack(@PathVariable long loanId, Authentication authentication) {
        return loanStore.giveBack(loanId, authentication.getName(), clock.instant());
    }

    @GetMapping("/api/loans/{loanId}")
    @PreAuthorize("hasAnyRole('RESPONSAVEL', 'ADMINISTRADOR')")
    public MaterialLoan find(@PathVariable long loanId) {
        return loanStore.findById(loanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));
    }

    @GetMapping("/api/loans")
    @PreAuthorize("hasAnyRole('RESPONSAVEL', 'ADMINISTRADOR')")
    public List<MaterialLoan> list(
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) Boolean open) {
        return loanStore.list(resourceId, open);
    }

    public record WithdrawalRequest(
            @NotBlank(message = "borrowerId is required") String borrowerId,
            String notes) {
    }
}
