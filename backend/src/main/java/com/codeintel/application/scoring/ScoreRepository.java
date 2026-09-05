package com.codeintel.application.scoring;

import com.codeintel.application.ports.outbound.GitIntelligenceStore;
import com.codeintel.application.ports.outbound.ScoringPort;
import com.codeintel.application.ports.outbound.ScoringPortException;
import com.codeintel.application.ports.outbound.ScoringStore;
import com.codeintel.application.ports.outbound.StaticAnalysisStore;
import com.codeintel.domain.repository.RepositoryId;
import com.codeintel.domain.scoring.ScoringResult;
import java.time.Clock;

public final class ScoreRepository {
    private final StaticAnalysisStore analyses;
    private final GitIntelligenceStore gitIntelligence;
    private final ScoringPort scorer;
    private final ScoringStore scores;
    private final Clock clock;

    public ScoreRepository(StaticAnalysisStore analyses, GitIntelligenceStore gitIntelligence,
            ScoringPort scorer, ScoringStore scores, Clock clock) {
        this.analyses = analyses;
        this.gitIntelligence = gitIntelligence;
        this.scorer = scorer;
        this.scores = scores;
        this.clock = clock;
    }

    public ScoringResult execute(RepositoryId repositoryId) {
        var analysis = analyses.findLatest(repositoryId)
                .orElseThrow(() -> new ScoringNotFoundException("static analysis not found"));
        var history = gitIntelligence.findLatest(repositoryId)
                .orElseThrow(() -> new ScoringNotFoundException("Git intelligence not found"));
        if (!analysis.acquisitionRevision().equals(history.acquisitionRevision())) {
            throw new ScoringValidationException("scoring inputs do not match the same exact revision");
        }
        try {
            var result = new ScoringResult(repositoryId, analysis.acquisitionRevision(),
                    scorer.score(analysis, history), clock.instant());
            scores.save(result);
            return result;
        } catch (ScoringPortException exception) {
            throw new ScoringValidationException(exception.getMessage());
        }
    }
}
