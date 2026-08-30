package com.codeintel.domain.repository;

import java.util.Objects;

public record GitHubAppConnection(long installationId, GitHubRepository repository)
        implements RepositoryConnection {
    public GitHubAppConnection {
        if (installationId <= 0) {
            throw new IllegalArgumentException("installationId must be positive");
        }
        Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public RepositorySourceType sourceType() {
        return RepositorySourceType.GITHUB_APP;
    }

    @Override
    public String safeLocator() {
        return "github.com/" + repository.fullName();
    }
}
