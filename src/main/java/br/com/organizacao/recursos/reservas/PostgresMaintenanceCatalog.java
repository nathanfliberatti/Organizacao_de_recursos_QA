package br.com.organizacao.recursos.reservas;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Persiste os periodos de manutencao (RF-08) no PostgreSQL; um periodo por recurso. */
@Component
@Primary
@Profile("postgres")
public class PostgresMaintenanceCatalog extends MaintenanceCatalog {
    private final JdbcTemplate jdbcTemplate;

    public PostgresMaintenanceCatalog(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void block(String resourceId, MaintenancePeriod period) {
        jdbcTemplate.update("""
                INSERT INTO maintenance_periods (resource_id, starts_at, ends_at) VALUES (?, ?, ?)
                ON CONFLICT (resource_id) DO UPDATE SET starts_at = EXCLUDED.starts_at, ends_at = EXCLUDED.ends_at
                """,
                resourceId, Timestamp.from(period.startsAt()), Timestamp.from(period.endsAt()));
    }

    @Override
    public MaintenancePeriod findByResourceId(String resourceId) {
        List<MaintenancePeriod> periods = jdbcTemplate.query(
                "SELECT starts_at, ends_at FROM maintenance_periods WHERE resource_id = ?",
                (resultSet, rowNumber) -> new MaintenancePeriod(
                        resultSet.getTimestamp("starts_at").toInstant(),
                        resultSet.getTimestamp("ends_at").toInstant()),
                resourceId);
        return periods.isEmpty() ? null : periods.getFirst();
    }

    @Override
    public void remove(String resourceId) {
        jdbcTemplate.update("DELETE FROM maintenance_periods WHERE resource_id = ?", resourceId);
    }

    @Override
    public Map<String, MaintenancePeriod> all() {
        Map<String, MaintenancePeriod> result = new TreeMap<>();
        jdbcTemplate.query("SELECT resource_id, starts_at, ends_at FROM maintenance_periods",
                resultSet -> {
                    result.put(resultSet.getString("resource_id"), new MaintenancePeriod(
                            resultSet.getTimestamp("starts_at").toInstant(),
                            resultSet.getTimestamp("ends_at").toInstant()));
                });
        return result;
    }

    @Override
    public void clear() {
        jdbcTemplate.update("DELETE FROM maintenance_periods");
    }
}
