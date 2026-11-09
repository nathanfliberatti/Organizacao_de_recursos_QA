package br.com.organizacao.recursos.reservas;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {
    private final NotificationStore store;

    public NotificationController(NotificationStore store) {
        this.store = store;
    }

    /** Cada usuario ve as proprias notificacoes; Responsavel e Administrador podem ver todas. */
    @GetMapping("/api/notifications")
    @PreAuthorize("isAuthenticated()")
    public List<Notification> list(
            @RequestParam(defaultValue = "false") boolean all,
            @RequestParam(defaultValue = "100") int limit,
            Authentication authentication) {
        boolean privileged = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_RESPONSAVEL"))
                || authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"));
        String recipient = all && privileged ? null : authentication.getName();
        return store.list(recipient, Math.max(1, Math.min(limit, 500)));
    }
}
