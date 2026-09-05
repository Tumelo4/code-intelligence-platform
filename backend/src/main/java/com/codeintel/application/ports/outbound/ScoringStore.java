package com.codeintel.application.ports.outbound;

import com.codeintel.domain.repository.RepositoryId;
import com.codeintel.domain.scoring.ScoringResult;
import java.util.Optional;

public interface ScoringStore {
    void save(ScoringResult result);
    Optional<ScoringResult> findLatest(RepositoryId repositoryId);
}
