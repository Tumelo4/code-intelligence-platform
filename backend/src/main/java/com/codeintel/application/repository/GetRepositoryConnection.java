package com.codeintel.application.repository;

import com.codeintel.application.ports.outbound.RepositoryStore;
import com.codeintel.domain.repository.RepositoryId;
import com.codeintel.domain.repository.ValidatedRepositoryConnection;
import java.util.Objects;

public final class GetRepositoryConnection {
    private final RepositoryStore repositoryStore;

    public GetRepositoryConnection(RepositoryStore repositoryStore) {
        this.repositoryStore = Objects.requireNonNull(repositoryStore);
    }

    public ValidatedRepositoryConnection execute(RepositoryId repositoryId) {
        return repositoryStore.find(Objects.requireNonNull(repositoryId))
                .orElseThrow(() -> new RepositoryConnectionNotFoundException(repositoryId));
    }
}
