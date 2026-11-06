package br.com.organizacao.recursos.reservas;

import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!perf & !test")
public class ProductUserRepository {
    private final JdbcTemplate jdbcTemplate;

    public ProductUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void lockAdministratorBootstrap() {
        jdbcTemplate.queryForObject(
                "SELECT pg_advisory_xact_lock(hashtextextended('product-admin-bootstrap', 0))",
                Object.class);
    }

    public boolean hasAdministrator() {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM product_user_roles WHERE role = 'ADMINISTRADOR'
                )
                """, Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    public Optional<ProductUser> findByLogin(String login) {
        List<ProductUser> matches = jdbcTemplate.query(
                "SELECT login, password_hash, active FROM product_users WHERE login = ?",
                (resultSet, rowNumber) -> new ProductUser(
                        resultSet.getString("login"),
                        resultSet.getString("password_hash"),
                        resultSet.getBoolean("active"),
                        findRoles(resultSet.getString("login"))),
                login);
        return matches.stream().findFirst();
    }

    public void insert(String login, String passwordHash, ProductUserRole role) {
        jdbcTemplate.update(
                "INSERT INTO product_users (login, password_hash, active) VALUES (?, ?, TRUE)",
                login,
                passwordHash);
        jdbcTemplate.update(
                "INSERT INTO product_user_roles (login, role) VALUES (?, ?)",
                login,
                role.name());
    }

    private List<ProductUserRole> findRoles(String login) {
        return jdbcTemplate.query(
                "SELECT role FROM product_user_roles WHERE login = ? ORDER BY role",
                (resultSet, rowNumber) -> ProductUserRole.valueOf(resultSet.getString("role")),
                login);
    }
}