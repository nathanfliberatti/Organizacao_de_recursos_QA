package br.com.organizacao.recursos.reservas;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
@Profile("postgres")
public class PostgresResourceStore implements ResourceStore {
    private static final String COLUMNS =
            "resource_id, name, resource_type, capacity, location, competency, restricted";
    private static final RowMapper<Resource> MAPPER = (resultSet, rowNumber) -> new Resource(
            resultSet.getString("resource_id"),
            resultSet.getString("name"),
            ResourceType.valueOf(resultSet.getString("resource_type")),
            resultSet.getInt("capacity"),
            resultSet.getString("location"),
            resultSet.getString("competency"),
            resultSet.getBoolean("restricted"));

    private final JdbcTemplate jdbcTemplate;

    public PostgresResourceStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Resource create(Resource resource) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO resources (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?)",
                    resource.id(),
                    resource.name(),
                    resource.type().name(),
                    resource.capacity(),
                    resource.location(),
                    resource.competency(),
                    resource.restricted());
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Resource already exists", exception);
        }
        return resource;
    }

    @Override
    public Optional<Resource> findById(String id) {
        return jdbcTemplate.query("SELECT " + COLUMNS + " FROM resources WHERE resource_id = ?", MAPPER, id)
                .stream().findFirst();
    }

    @Override
    public List<Resource> search(ResourceFilter filter) {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM resources WHERE TRUE");
        List<Object> arguments = new ArrayList<>();
        if (filter.type() != null) {
            sql.append(" AND resource_type = ?");
            arguments.add(filter.type().name());
        }
        if (filter.minCapacity() != null) {
            sql.append(" AND capacity >= ?");
            arguments.add(filter.minCapacity());
        }
        if (filter.location() != null && !filter.location().isBlank()) {
            sql.append(" AND location ILIKE ?");
            arguments.add(like(filter.location()));
        }
        if (filter.competency() != null && !filter.competency().isBlank()) {
            sql.append(" AND competency ILIKE ?");
            arguments.add(like(filter.competency()));
        }
        if (filter.text() != null && !filter.text().isBlank()) {
            sql.append(" AND (resource_id ILIKE ? OR name ILIKE ?)");
            arguments.add(like(filter.text()));
            arguments.add(like(filter.text()));
        }
        sql.append(" ORDER BY resource_id");
        return jdbcTemplate.query(sql.toString(), MAPPER, arguments.toArray());
    }

    @Override
    public Resource update(Resource resource) {
        int updated = jdbcTemplate.update("""
                UPDATE resources
                SET name = ?, resource_type = ?, capacity = ?, location = ?, competency = ?, restricted = ?
                WHERE resource_id = ?
                """,
                resource.name(),
                resource.type().name(),
                resource.capacity(),
                resource.location(),
                resource.competency(),
                resource.restricted(),
                resource.id());
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        }
        return resource;
    }

    private static String like(String text) {
        String escaped = text.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
