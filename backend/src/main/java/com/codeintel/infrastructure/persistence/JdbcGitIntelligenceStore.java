package com.codeintel.infrastructure.persistence;

import com.codeintel.application.ports.outbound.GitIntelligenceStore;
import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.git.GitIntelligenceReport;
import com.codeintel.domain.git.GitIntelligenceResult;
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
public class JdbcGitIntelligenceStore implements GitIntelligenceStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public JdbcGitIntelligenceStore(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }
    @Override public void save(GitIntelligenceResult result) {
        jdbc.update("""
                INSERT INTO git_intelligence (repository_id, revision_kind, revision_value, report, analyzed_at)
                VALUES (?, ?, ?, ?::jsonb, ?)
                ON CONFLICT (repository_id, revision_kind, revision_value)
                DO UPDATE SET report = EXCLUDED.report, analyzed_at = EXCLUDED.analyzed_at
                """, result.repositoryId().value(), result.acquisitionRevision().kind().name(),
                result.acquisitionRevision().value(), write(result.report()), Timestamp.from(result.analyzedAt()));
    }
    @Override public Optional<GitIntelligenceResult> findLatest(RepositoryId repositoryId) {
        return jdbc.query("""
                SELECT repository_id, revision_kind, revision_value, report::text, analyzed_at
                FROM git_intelligence WHERE repository_id = ? ORDER BY analyzed_at DESC LIMIT 1
                """, this::map, repositoryId.value()).stream().findFirst();
    }
    private GitIntelligenceResult map(ResultSet rs, int row) throws SQLException {
        try {
            return new GitIntelligenceResult(new RepositoryId(rs.getObject("repository_id", java.util.UUID.class)),
                    new AcquisitionRevision(AcquisitionRevision.Kind.valueOf(rs.getString("revision_kind")),
                            rs.getString("revision_value")),
                    mapper.readValue(rs.getString("report"), GitIntelligenceReport.class),
                    rs.getTimestamp("analyzed_at").toInstant());
        } catch (JsonProcessingException exception) {
            throw new SQLException("stored Git intelligence is invalid", exception);
        }
    }
    private String write(GitIntelligenceReport report) {
        try { return mapper.writeValueAsString(report); }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("Git intelligence serialization failed", exception);
        }
    }
}
