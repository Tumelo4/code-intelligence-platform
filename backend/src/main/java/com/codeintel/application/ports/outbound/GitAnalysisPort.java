package com.codeintel.application.ports.outbound;

import com.codeintel.domain.repository.RepositoryRevision;

public interface GitAnalysisPort {
    void analyze(RepositoryRevision revision);
}
