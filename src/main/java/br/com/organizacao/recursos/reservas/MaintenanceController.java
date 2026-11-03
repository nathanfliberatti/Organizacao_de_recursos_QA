package br.com.organizacao.recursos.reservas;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MaintenanceController {
    private final MaintenanceCatalog maintenanceCatalog;

    public MaintenanceController(MaintenanceCatalog maintenanceCatalog) {
        this.maintenanceCatalog = maintenanceCatalog;
    }

    @PostMapping("/api/resources/{resourceId}/maintenance")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> block(@PathVariable String resourceId, @RequestBody MaintenancePeriod period) {
        maintenanceCatalog.block(resourceId, period);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
