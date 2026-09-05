package com.codeintel.infrastructure.persistence;

import com.codeintel.application.ports.outbound.ScoringStore;
import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.repository.RepositoryId;
import com.codeintel.domain.scoring.ScoringReport;
import com.codeintel.domain.scoring.ScoringResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcScoringStore implements ScoringStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public JdbcScoringStore(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }
    @Override public void save(ScoringResult result) {
        jdbc.update("""
                INSERT INTO repository_scoring (repository_id, revision_kind, revision_value, report, scored_at)
                VALUES (?, ?, ?, ?::jsonb, ?)
                ON CONFLICT (repository_id, revision_kind, revision_value)
                DO UPDATE SET report = EXCLUDED.report, scored_at = EXCLUDED.scored_at
                """, result.repositoryId().value(), result.acquisitionRevision().kind().name(),
                result.acquisitionRevision().value(), write(result.report()), Timestamp.from(result.scoredAt()));
    }
    @Override public Optional<ScoringResult> findLatest(RepositoryId repositoryId) {
        return jdbc.query("""
                SELECT repository_id, revision_kind, revision_value, report::text, scored_at
                FROM repository_scoring WHERE repository_id = ? ORDER BY scored_at DESC LIMIT 1
                """, this::map, repositoryId.value()).stream().findFirst();
    }
    private ScoringResult map(ResultSet rs, int row) throws SQLException {
        try {
            return new ScoringResult(new RepositoryId(rs.getObject("repository_id", java.util.UUID.class)),
                    new AcquisitionRevision(AcquisitionRevision.Kind.valueOf(rs.getString("revision_kind")),
                            rs.getString("revision_value")),
                    mapper.readValue(rs.getString("report"), ScoringReport.class),
                    rs.getTimestamp("scored_at").toInstant());
        } catch (JsonProcessingException exception) {
            throw new SQLException("stored scoring is invalid", exception);
        }
    }
    private String write(ScoringReport report) {
        try { return mapper.writeValueAsString(report); }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("scoring serialization failed", exception);
        }
    }
}
