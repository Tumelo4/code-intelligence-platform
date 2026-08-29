package com.codeintel.domain.repository;

import java.util.Objects;

public record RepositoryRevision(RepositoryId repositoryId, String commitSha) {
    public RepositoryRevision {
        Objects.requireNonNull(repositoryId, "repositoryId must not be null");
        if (commitSha == null || !commitSha.matches("[0-9a-fA-F]{40}")) {
            throw new IllegalArgumentException("commitSha must be a 40-character hexadecimal SHA");
        }
    }
}
