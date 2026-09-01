package com.codeintel.application.ports.outbound;

import com.codeintel.domain.analysis.StaticAnalysisResult;
import com.codeintel.domain.repository.RepositoryId;
import java.util.Optional;

public interface StaticAnalysisStore {
    void save(StaticAnalysisResult result);
    Optional<StaticAnalysisResult> findLatest(RepositoryId repositoryId);
}
