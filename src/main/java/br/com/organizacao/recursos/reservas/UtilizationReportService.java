package br.com.organizacao.recursos.reservas;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("postgres")
public class UtilizationReportService {
    private final JdbcTemplate jdbcTemplate;

    public UtilizationReportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public UtilizationReportResponse report(Instant inicio, Instant fim) {
        if (!inicio.isBefore(fim)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'inicio' deve ser anterior a 'fim'");
        }

        Timestamp start = Timestamp.from(inicio);
        Timestamp end = Timestamp.from(fim);
        List<UtilizationReportResponse.ResourceUsage> resourceHours = jdbcTemplate.query("""
                SELECT room_id,
                       SUM(EXTRACT(EPOCH FROM (LEAST(ends_at, ?) - GREATEST(starts_at, ?))) / 3600)
                           AS reserved_hours
                FROM reservations
                WHERE status <> 'CANCELADA' AND starts_at < ? AND ends_at > ?
                GROUP BY room_id
                ORDER BY room_id
                """,
                (resultSet, rowNumber) -> new UtilizationReportResponse.ResourceUsage(
                        resultSet.getString("room_id"),
                        resultSet.getBigDecimal("reserved_hours")),
                end,
                start,
                end,
                start);

        List<UtilizationReportResponse.ProfessorWorkload> professorHours = jdbcTemplate.query("""
                SELECT professor_id,
                       SUM(EXTRACT(EPOCH FROM (LEAST(ends_at, ?) - GREATEST(starts_at, ?))) / 3600)
                           AS allocated_hours
                FROM reservations
                WHERE status <> 'CANCELADA' AND starts_at < ? AND ends_at > ?
                GROUP BY professor_id
                ORDER BY professor_id
                """,
                (resultSet, rowNumber) -> new UtilizationReportResponse.ProfessorWorkload(
                        resultSet.getString("professor_id"),
                        resultSet.getBigDecimal("allocated_hours")),
                end,
                start,
                end,
                start);

        long conflictCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM reservation_conflict_attempts
                WHERE starts_at < ? AND ends_at > ?
                """,
                Long.class,
                end,
                start);

        return new UtilizationReportResponse(
                inicio, fim, resourceHours, professorHours, conflictCount);
    }
}
