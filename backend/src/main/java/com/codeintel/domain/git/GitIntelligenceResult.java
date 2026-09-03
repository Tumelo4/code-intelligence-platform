package com.codeintel.domain.git;

import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.repository.RepositoryId;
import java.time.Instant;

public record GitIntelligenceResult(RepositoryId repositoryId, AcquisitionRevision acquisitionRevision,
        GitIntelligenceReport report, Instant analyzedAt) {
    public GitIntelligenceResult {
        if (repositoryId == null || acquisitionRevision == null || report == null || analyzedAt == null) {
            throw new IllegalArgumentException("git intelligence result fields are required");
        }
    }
}
