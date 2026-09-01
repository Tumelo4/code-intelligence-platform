package com.codeintel.infrastructure.persistence;

import com.codeintel.application.ports.outbound.RepositoryInventoryStore;
import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.inventory.InventoryReport;
import com.codeintel.domain.inventory.RepositoryInventory;
import com.codeintel.domain.repository.RepositoryId;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRepositoryInventoryStore implements RepositoryInventoryStore {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcRepositoryInventoryStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(RepositoryInventory inventory) {
        jdbcTemplate.update("""
                INSERT INTO repository_inventory
                    (repository_id, revision_kind, revision_value, report, inventoried_at)
                VALUES (?, ?, ?, ?::jsonb, ?)
                ON CONFLICT (repository_id, revision_kind, revision_value)
                DO UPDATE SET report = EXCLUDED.report, inventoried_at = EXCLUDED.inventoried_at
                """, inventory.repositoryId().value(), inventory.acquisitionRevision().kind().name(),
                inventory.acquisitionRevision().value(), write(inventory.report()),
                Timestamp.from(inventory.inventoriedAt()));
    }

    @Override
    public Optional<RepositoryInventory> findLatest(RepositoryId repositoryId) {
        return jdbcTemplate.query("""
                SELECT repository_id, revision_kind, revision_value, report::text, inventoried_at
                FROM repository_inventory
                WHERE repository_id = ? ORDER BY inventoried_at DESC LIMIT 1
                """, this::map, repositoryId.value()).stream().findFirst();
    }

    private RepositoryInventory map(ResultSet resultSet, int rowNumber) throws SQLException {
        try {
            return new RepositoryInventory(
                    new RepositoryId(resultSet.getObject("repository_id", java.util.UUID.class)),
                    new AcquisitionRevision(
                            AcquisitionRevision.Kind.valueOf(resultSet.getString("revision_kind")),
                            resultSet.getString("revision_value")),
                    objectMapper.readValue(resultSet.getString("report"), InventoryReport.class),
                    resultSet.getTimestamp("inventoried_at").toInstant());
        } catch (JsonProcessingException exception) {
            throw new SQLException("stored repository inventory is invalid", exception);
        }
    }

    private String write(InventoryReport report) {
        try {
            return objectMapper.writeValueAsString(report);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("repository inventory cannot be serialized", exception);
        }
    }
}
