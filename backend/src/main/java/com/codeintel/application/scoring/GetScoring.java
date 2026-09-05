package com.codeintel.application.scoring;

import com.codeintel.application.ports.outbound.ScoringStore;
import com.codeintel.domain.repository.RepositoryId;
import com.codeintel.domain.scoring.ScoringResult;

public final class GetScoring {
    private final ScoringStore scores;
    public GetScoring(ScoringStore scores) { this.scores = scores; }
    public ScoringResult execute(RepositoryId repositoryId) {
        return scores.findLatest(repositoryId)
                .orElseThrow(() -> new ScoringNotFoundException("repository scoring not found"));
    }
}
