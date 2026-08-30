package com.codeintel.application.repository;

import com.codeintel.domain.repository.RepositoryId;

public final class RepositoryConnectionNotFoundException extends RuntimeException {
    public RepositoryConnectionNotFoundException(RepositoryId repositoryId) {
        super("repository connection not found: " + repositoryId.value());
    }
}
