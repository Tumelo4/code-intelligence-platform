package com.codeintel.infrastructure.repository;

import com.codeintel.domain.repository.GitHubRepository;

@FunctionalInterface
public interface GitHubAppAccessProbe {
    boolean canRead(long installationId, GitHubRepository repository);
}
