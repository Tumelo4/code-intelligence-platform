package com.codeintel.application.ports.outbound;

import com.codeintel.domain.git.GitIntelligenceResult;
import com.codeintel.domain.repository.RepositoryId;
import java.util.Optional;

public interface GitIntelligenceStore {
    void save(GitIntelligenceResult result);
    Optional<GitIntelligenceResult> findLatest(RepositoryId repositoryId);
}
