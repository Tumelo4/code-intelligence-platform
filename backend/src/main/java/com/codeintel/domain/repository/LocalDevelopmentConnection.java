package com.codeintel.domain.repository;

import java.nio.file.Path;
import java.util.Objects;

public record LocalDevelopmentConnection(Path repositoryPath) implements RepositoryConnection {
    public LocalDevelopmentConnection {
        Objects.requireNonNull(repositoryPath, "repositoryPath must not be null");
        if (!repositoryPath.isAbsolute()) {
            throw new IllegalArgumentException("local development path must be absolute");
        }
        repositoryPath = repositoryPath.normalize();
    }

    @Override
    public RepositorySourceType sourceType() {
        return RepositorySourceType.LOCAL_DEVELOPMENT_PATH;
    }

    @Override
    public String safeLocator() {
        return repositoryPath.toString();
    }
}
