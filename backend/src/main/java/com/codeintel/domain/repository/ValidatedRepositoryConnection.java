package com.codeintel.domain.repository;

import java.time.Instant;
import java.util.Objects;

public record ValidatedRepositoryConnection(
        RepositoryId repositoryId,
        RepositorySourceType sourceType,
        String safeLocator,
        Instant validatedAt) {
    public ValidatedRepositoryConnection {
        Objects.requireNonNull(repositoryId, "repositoryId must not be null");
        Objects.requireNonNull(sourceType, "sourceType must not be null");
        if (safeLocator == null || safeLocator.isBlank()) {
            throw new IllegalArgumentException("safeLocator must not be blank");
        }
        Objects.requireNonNull(validatedAt, "validatedAt must not be null");
    }
}
