package br.com.organizacao.recursos.reservas;

import java.time.Instant;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("postgres")
public class UtilizationReportController {
    private final UtilizationReportService utilizationReportService;

    public UtilizationReportController(UtilizationReportService utilizationReportService) {
        this.utilizationReportService = utilizationReportService;
    }

    @GetMapping("/relatorios/utilizacao")
    @PreAuthorize("hasAnyRole('RESPONSAVEL', 'ADMINISTRADOR')")
    public UtilizationReportResponse utilization(
            @RequestParam Instant inicio,
            @RequestParam Instant fim) {
        return utilizationReportService.report(inicio, fim);
    }
}
