package com.codeintel.application.git;

import com.codeintel.application.ports.outbound.AcquisitionRecordStore;
import com.codeintel.application.ports.outbound.GitAnalysisPort;
import com.codeintel.application.ports.outbound.GitIntelligenceStore;
import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.git.GitIntelligenceResult;
import com.codeintel.domain.repository.RepositoryId;
import java.time.Clock;

public final class AnalyzeGitIntelligence {
    private final AcquisitionRecordStore acquisitions;
    private final GitAnalysisPort analyzer;
    private final GitIntelligenceStore results;
    private final Clock clock;

    public AnalyzeGitIntelligence(AcquisitionRecordStore acquisitions, GitAnalysisPort analyzer,
            GitIntelligenceStore results, Clock clock) {
        this.acquisitions = acquisitions;
        this.analyzer = analyzer;
        this.results = results;
        this.clock = clock;
    }

    public GitIntelligenceResult execute(RepositoryId repositoryId) {
        var acquisition = acquisitions.findLatest(repositoryId)
                .orElseThrow(() -> new GitIntelligenceNotFoundException("repository acquisition not found"));
        if (acquisition.revision().kind() != AcquisitionRevision.Kind.GIT_COMMIT) {
            throw new GitIntelligenceValidationException("latest acquisition does not contain Git history");
        }
        var result = new GitIntelligenceResult(repositoryId, acquisition.revision(),
                analyzer.analyze(acquisition.immutableOriginal(), acquisition.revision()), clock.instant());
        results.save(result);
        return result;
    }
}
