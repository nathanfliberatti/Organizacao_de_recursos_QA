package br.com.organizacao.recursos.reservas;

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
}
