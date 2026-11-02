package br.com.organizacao.recursos.reservas;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record UtilizationReportResponse(
        Instant inicio,
        Instant fim,
        List<ResourceUsage> horasPorRecurso,
        List<ProfessorWorkload> cargaPorProfessor,
        long conflitosEvitados) {

    public record ResourceUsage(String recursoId, BigDecimal horasReservadas) {
    }

    public record ProfessorWorkload(String professorId, BigDecimal horasAlocadas) {
    }
}
