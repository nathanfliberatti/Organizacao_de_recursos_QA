package br.com.organizacao.recursos.reservas;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class PostgresNotificationStore implements NotificationStore {
    private static final RowMapper<Notification> MAPPER = (resultSet, rowNumber) -> new Notification(
            resultSet.getLong("notification_id"),
            NotificationEvent.valueOf(resultSet.getString("event_type")),
            resultSet.getString("reservation_id"),
            resultSet.getString("recipient_id"),
            resultSet.getString("message"),
            resultSet.getString("delivery"),
            resultSet.getTimestamp("created_at").toInstant());

    private final JdbcTemplate jdbcTemplate;

    public PostgresNotificationStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Notification save(Notification notification) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO notifications (event_type, reservation_id, recipient_id, message, delivery, created_at)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING notification_id
                """,
                Long.class,
                notification.event().name(), notification.reservationId(), notification.recipientId(),
                notification.message(), notification.delivery(), Timestamp.from(notification.createdAt()));
        return new Notification(Objects.requireNonNull(id), notification.event(), notification.reservationId(),
                notification.recipientId(), notification.message(), notification.delivery(),
                notification.createdAt());
    }

    @Override
    public List<Notification> list(String recipientId, int limit) {
        String where = recipientId == null ? "" : "WHERE recipient_id = ? ";
        String sql = "SELECT notification_id, event_type, reservation_id, recipient_id, message, delivery, created_at"
                + " FROM notifications " + where + "ORDER BY notification_id DESC LIMIT ?";
        return recipientId == null
                ? jdbcTemplate.query(sql, MAPPER, limit)
                : jdbcTemplate.query(sql, MAPPER, recipientId, limit);
    }
}
