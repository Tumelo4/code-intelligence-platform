package com.codeintel.application.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeintel.application.ports.outbound.GitIntelligenceStore;
import com.codeintel.application.ports.outbound.ScoringStore;
import com.codeintel.application.ports.outbound.StaticAnalysisStore;
import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.analysis.AnalysisReport;
import com.codeintel.domain.analysis.StaticAnalysisResult;
import com.codeintel.domain.git.GitIntelligenceReport;
import com.codeintel.domain.git.GitIntelligenceResult;
import com.codeintel.domain.repository.RepositoryId;
import com.codeintel.domain.scoring.ScoringReport;
import com.codeintel.domain.scoring.ScoringResult;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ScoreRepositoryTest {
    @Test
    void scoresMatchingLatestInputsAndPersistsExactRevision() {
        RepositoryId id = new RepositoryId(UUID.randomUUID());
        var revision = new AcquisitionRevision(AcquisitionRevision.Kind.GIT_COMMIT, "a".repeat(40));
        var analysis = new StaticAnalysisResult(id, revision,
                new AnalysisReport(List.of(), List.of()), Instant.EPOCH);
        var history = new GitIntelligenceResult(id, revision,
                new GitIntelligenceReport(List.of(), List.of(), List.of(), false), Instant.EPOCH);
        var store = new CapturingScoringStore();
        Instant scoredAt = Instant.parse("2026-09-05T00:00:00Z");
        var useCase = new ScoreRepository(analyses(analysis), histories(history), (actualAnalysis, actualHistory) -> {
            assertThat(actualAnalysis).isEqualTo(analysis);
            assertThat(actualHistory).isEqualTo(history);
            return new ScoringReport(100, List.of(), List.of());
        }, store, Clock.fixed(scoredAt, ZoneOffset.UTC));

        ScoringResult result = useCase.execute(id);

        assertThat(result.acquisitionRevision()).isEqualTo(revision);
        assertThat(result.scoredAt()).isEqualTo(scoredAt);
        assertThat(store.value).isEqualTo(result);
    }

    @Test
    void rejectsMismatchedRevisionsBeforeScoring() {
        RepositoryId id = new RepositoryId(UUID.randomUUID());
        var analysisRevision = new AcquisitionRevision(AcquisitionRevision.Kind.GIT_COMMIT, "a".repeat(40));
        var historyRevision = new AcquisitionRevision(AcquisitionRevision.Kind.GIT_COMMIT, "b".repeat(40));
        var analysis = new StaticAnalysisResult(id, analysisRevision,
                new AnalysisReport(List.of(), List.of()), Instant.EPOCH);
        var history = new GitIntelligenceResult(id, historyRevision,
                new GitIntelligenceReport(List.of(), List.of(), List.of(), false), Instant.EPOCH);
        var useCase = new ScoreRepository(analyses(analysis), histories(history),
                (ignoredAnalysis, ignoredHistory) -> { throw new AssertionError("must not score"); },
                new CapturingScoringStore(), Clock.systemUTC());

        assertThatThrownBy(() -> useCase.execute(id))
                .isInstanceOf(ScoringValidationException.class)
                .hasMessageContaining("same exact revision");
    }

    private static StaticAnalysisStore analyses(StaticAnalysisResult value) {
        return new StaticAnalysisStore() {
            public void save(StaticAnalysisResult ignored) { }
            public Optional<StaticAnalysisResult> findLatest(RepositoryId ignored) { return Optional.of(value); }
        };
    }

    private static GitIntelligenceStore histories(GitIntelligenceResult value) {
        return new GitIntelligenceStore() {
            public void save(GitIntelligenceResult ignored) { }
            public Optional<GitIntelligenceResult> findLatest(RepositoryId ignored) { return Optional.of(value); }
        };
    }

    private static final class CapturingScoringStore implements ScoringStore {
        private ScoringResult value;
        public void save(ScoringResult result) { value = result; }
        public Optional<ScoringResult> findLatest(RepositoryId ignored) { return Optional.ofNullable(value); }
    }
}
