package com.codeintel.infrastructure.persistence;

import com.codeintel.application.ports.outbound.AcquisitionRecordStore;
import com.codeintel.domain.acquisition.AcquiredRepository;
import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.repository.RepositoryId;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAcquisitionRecordStore implements AcquisitionRecordStore {
    private final JdbcTemplate jdbcTemplate;

    public JdbcAcquisitionRecordStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(AcquiredRepository acquisition) {
        jdbcTemplate.update("""
                INSERT INTO repository_acquisition
                    (repository_id, revision_kind, revision_value, requested_revision,
                     immutable_original, working_copy, skipped_submodules, acquired_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, acquisition.repositoryId().value(), acquisition.revision().kind().name(),
                acquisition.revision().value(), acquisition.requestedRevision(),
                acquisition.immutableOriginal().toString(), acquisition.workingCopy().toString(),
                acquisition.skippedSubmodules(), Timestamp.from(acquisition.acquiredAt()));
    }

    @Override
    public Optional<AcquiredRepository> findLatest(RepositoryId repositoryId) {
        return jdbcTemplate.query("""
                SELECT repository_id, revision_kind, revision_value, requested_revision,
                       immutable_original, working_copy, skipped_submodules, acquired_at
                FROM repository_acquisition
                WHERE repository_id = ? ORDER BY acquired_at DESC LIMIT 1
                """, this::map, repositoryId.value()).stream().findFirst();
    }

    private AcquiredRepository map(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AcquiredRepository(
                new RepositoryId(resultSet.getObject("repository_id", java.util.UUID.class)),
                new AcquisitionRevision(
                        AcquisitionRevision.Kind.valueOf(resultSet.getString("revision_kind")),
                        resultSet.getString("revision_value")),
                resultSet.getString("requested_revision"),
                Path.of(resultSet.getString("immutable_original")),
                Path.of(resultSet.getString("working_copy")),
                resultSet.getInt("skipped_submodules"),
                resultSet.getTimestamp("acquired_at").toInstant());
    }
}
