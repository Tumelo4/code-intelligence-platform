package com.codeintel.application.git;

import com.codeintel.application.ports.outbound.GitIntelligenceStore;
import com.codeintel.domain.git.GitIntelligenceResult;
import com.codeintel.domain.repository.RepositoryId;

public final class GetGitIntelligence {
    private final GitIntelligenceStore store;
    public GetGitIntelligence(GitIntelligenceStore store) { this.store = store; }
    public GitIntelligenceResult execute(RepositoryId repositoryId) {
        return store.findLatest(repositoryId)
                .orElseThrow(() -> new GitIntelligenceNotFoundException("Git intelligence not found"));
    }
}
