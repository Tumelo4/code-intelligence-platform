package com.codeintel.infrastructure.persistence;

import com.codeintel.application.ports.outbound.StaticAnalysisStore;
import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.analysis.AnalysisReport;
import com.codeintel.domain.analysis.StaticAnalysisResult;
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
public class JdbcStaticAnalysisStore implements StaticAnalysisStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public JdbcStaticAnalysisStore(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; }
    public void save(StaticAnalysisResult result) {
        jdbc.update("""
                INSERT INTO static_analysis (repository_id, revision_kind, revision_value, report, analyzed_at)
                VALUES (?, ?, ?, ?::jsonb, ?)
                ON CONFLICT (repository_id, revision_kind, revision_value)
                DO UPDATE SET report = EXCLUDED.report, analyzed_at = EXCLUDED.analyzed_at
                """, result.repositoryId().value(), result.acquisitionRevision().kind().name(),
                result.acquisitionRevision().value(), write(result.report()), Timestamp.from(result.analyzedAt()));
    }
    public Optional<StaticAnalysisResult> findLatest(RepositoryId id) {
        return jdbc.query("""
                SELECT repository_id, revision_kind, revision_value, report::text, analyzed_at
                FROM static_analysis WHERE repository_id = ? ORDER BY analyzed_at DESC LIMIT 1
                """, this::map, id.value()).stream().findFirst();
    }
    private StaticAnalysisResult map(ResultSet rs, int row) throws SQLException {
        try {
            return new StaticAnalysisResult(new RepositoryId(rs.getObject("repository_id", java.util.UUID.class)),
                    new AcquisitionRevision(AcquisitionRevision.Kind.valueOf(rs.getString("revision_kind")),
                            rs.getString("revision_value")),
                    mapper.readValue(rs.getString("report"), AnalysisReport.class),
                    rs.getTimestamp("analyzed_at").toInstant());
        } catch (JsonProcessingException exception) { throw new SQLException("stored analysis is invalid", exception); }
    }
    private String write(AnalysisReport report) {
        try { return mapper.writeValueAsString(report); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("analysis serialization failed", exception); }
    }
}
