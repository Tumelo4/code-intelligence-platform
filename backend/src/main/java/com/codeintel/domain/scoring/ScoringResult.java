package com.codeintel.domain.scoring;

import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.repository.RepositoryId;
import java.time.Instant;

public record ScoringResult(RepositoryId repositoryId, AcquisitionRevision acquisitionRevision,
        ScoringReport report, Instant scoredAt) {
    public ScoringResult {
        if (repositoryId == null || acquisitionRevision == null || report == null || scoredAt == null) {
            throw new IllegalArgumentException("scoring result fields are required");
        }
    }
}
