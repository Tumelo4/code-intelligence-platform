package com.codeintel.domain.analysis;

import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.repository.RepositoryId;
import java.time.Instant;

public record StaticAnalysisResult(RepositoryId repositoryId, AcquisitionRevision acquisitionRevision,
        AnalysisReport report, Instant analyzedAt) {
    public StaticAnalysisResult {
        if (repositoryId == null || acquisitionRevision == null || report == null || analyzedAt == null) {
            throw new IllegalArgumentException("static analysis result fields are required");
        }
    }
}
