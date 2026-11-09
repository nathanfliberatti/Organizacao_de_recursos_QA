package br.com.organizacao.recursos.reservas;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class MaintenanceController {
    private final MaintenanceCatalog maintenanceCatalog;

    public MaintenanceController(MaintenanceCatalog maintenanceCatalog) {
        this.maintenanceCatalog = maintenanceCatalog;
    }

    @PostMapping("/api/resources/{resourceId}/maintenance")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> block(@PathVariable String resourceId, @RequestBody MaintenancePeriod period) {
        if (period == null || period.startsAt() == null || period.endsAt() == null
                || !period.startsAt().isBefore(period.endsAt())) {
            throw new InvalidReservationPeriodException();
        }
        maintenanceCatalog.block(resourceId, period);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/api/resources/{resourceId}/maintenance")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> unblock(@PathVariable String resourceId) {
        if (maintenanceCatalog.findByResourceId(resourceId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Maintenance period not found");
        }
        maintenanceCatalog.remove(resourceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/maintenance")
    @PreAuthorize("isAuthenticated()")
    public List<MaintenanceEntry> list() {
        return maintenanceCatalog.all().entrySet().stream()
                .map(entry -> new MaintenanceEntry(
                        entry.getKey(), entry.getValue().startsAt(), entry.getValue().endsAt()))
                .toList();
    }

    public record MaintenanceEntry(String resourceId, java.time.Instant startsAt, java.time.Instant endsAt) {
    }
}
