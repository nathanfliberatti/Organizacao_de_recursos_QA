package br.com.organizacao.recursos.reservas;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
@Profile("postgres")
public class PostgresResourceStore implements ResourceStore {
    private final JdbcTemplate jdbcTemplate;

    public PostgresResourceStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Resource create(Resource resource) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO resources (resource_id, capacity) VALUES (?, ?)",
                    resource.id(),
                    resource.capacity());
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Resource already exists", exception);
        }
        return resource;
    }

    @Override
    public List<Resource> findAll() {
        return jdbcTemplate.query(
                "SELECT resource_id, capacity FROM resources ORDER BY resource_id",
                (resultSet, rowNumber) -> new Resource(
                        resultSet.getString("resource_id"), resultSet.getInt("capacity")));
    }
}
