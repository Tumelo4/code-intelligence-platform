package com.codeintel.infrastructure.persistence;

import com.codeintel.application.ports.outbound.RepositoryStore;
import com.codeintel.domain.repository.RepositoryId;
import com.codeintel.domain.repository.RepositorySourceType;
import com.codeintel.domain.repository.ValidatedRepositoryConnection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRepositoryStore implements RepositoryStore {
    private final JdbcTemplate jdbcTemplate;

    public JdbcRepositoryStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(ValidatedRepositoryConnection connection) {
        jdbcTemplate.update("""
                INSERT INTO repository_connection
                    (repository_id, source_type, safe_locator, validated_at)
                VALUES (?, ?, ?, ?)
                """, connection.repositoryId().value(), connection.sourceType().name(),
                connection.safeLocator(), Timestamp.from(connection.validatedAt()));
    }

    @Override
    public Optional<ValidatedRepositoryConnection> find(RepositoryId repositoryId) {
        return jdbcTemplate.query("""
                        SELECT repository_id, source_type, safe_locator, validated_at
                        FROM repository_connection WHERE repository_id = ?
                        """, this::map, repositoryId.value()).stream().findFirst();
    }

    private ValidatedRepositoryConnection map(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ValidatedRepositoryConnection(
                new RepositoryId(resultSet.getObject("repository_id", java.util.UUID.class)),
                RepositorySourceType.valueOf(resultSet.getString("source_type")),
                resultSet.getString("safe_locator"),
                resultSet.getTimestamp("validated_at").toInstant());
    }
}
