package com.codeintel.application.ports.outbound;

import com.codeintel.domain.repository.RepositoryId;
import com.codeintel.domain.repository.ValidatedRepositoryConnection;
import java.util.Optional;

public interface RepositoryStore {
    void save(ValidatedRepositoryConnection connection);

    Optional<ValidatedRepositoryConnection> find(RepositoryId repositoryId);
}
