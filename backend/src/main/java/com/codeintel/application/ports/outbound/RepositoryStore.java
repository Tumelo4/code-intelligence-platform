package com.codeintel.application.ports.outbound;

import com.codeintel.domain.repository.RepositoryId;
import com.codeintel.domain.repository.RepositoryConnection;
import com.codeintel.domain.repository.ValidatedRepositoryConnection;
import java.util.Optional;

public interface RepositoryStore {
    void save(ValidatedRepositoryConnection connection, RepositoryConnection source);

    Optional<ValidatedRepositoryConnection> find(RepositoryId repositoryId);

    Optional<RepositoryConnection> findSource(RepositoryId repositoryId);
}
