package br.com.organizacao.recursos.reservas;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Listagem de reservas: o Solicitante ve as proprias; Responsavel e Administrador veem todas. */
@RestController
public class ReservationQueryController {
    private final ReservationExtras extras;

    public ReservationQueryController(ReservationExtras extras) {
        this.extras = extras;
    }

    @GetMapping("/api/reservations")
    @PreAuthorize("isAuthenticated()")
    public List<ReservationSummary> list(
            @RequestParam(defaultValue = "false") boolean mine, Authentication authentication) {
        boolean canSeeAll = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_RESPONSAVEL"))
                || authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"));
        return extras.list(canSeeAll && !mine ? null : authentication.getName());
    }
}
