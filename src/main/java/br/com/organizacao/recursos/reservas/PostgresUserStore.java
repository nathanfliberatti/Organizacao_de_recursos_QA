package br.com.organizacao.recursos.reservas;

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
@Profile("postgres & !perf")
public class PostgresUserStore implements UserStore {
    private static final RowMapper<AppUser> MAPPER = (resultSet, rowNumber) -> new AppUser(
            resultSet.getString("username"),
            resultSet.getString("password_hash"),
            resultSet.getString("display_name"),
            UserRole.valueOf(resultSet.getString("role")),
            resultSet.getBoolean("active"));

    private final JdbcTemplate jdbcTemplate;

    public PostgresUserStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<AppUser> findByUsername(String username) {
        return jdbcTemplate.query("""
                SELECT username, password_hash, display_name, role, active FROM app_users WHERE username = ?
                """, MAPPER, username).stream().findFirst();
    }

    @Override
    public List<AppUser> findAll() {
        return jdbcTemplate.query("""
                SELECT username, password_hash, display_name, role, active FROM app_users ORDER BY username
                """, MAPPER);
    }

    @Override
    public AppUser create(AppUser user) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO app_users (username, password_hash, display_name, role, active)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    user.username(), user.passwordHash(), user.displayName(), user.role().name(), user.active());
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists", exception);
        }
        return user;
    }

    @Override
    public long count() {
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_users", Long.class);
        return total == null ? 0 : total;
    }
}
