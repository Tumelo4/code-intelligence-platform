package com.codeintel.application.repository;

import com.codeintel.application.ports.outbound.RepositoryConnectionPort;
import com.codeintel.application.ports.outbound.RepositoryStore;
import com.codeintel.domain.repository.RepositoryConnection;
import com.codeintel.domain.repository.ValidatedRepositoryConnection;
import java.util.Objects;

public final class ConnectRepository {
    private final RepositoryConnectionPort connectionPort;
    private final RepositoryStore repositoryStore;

    public ConnectRepository(RepositoryConnectionPort connectionPort, RepositoryStore repositoryStore) {
        this.connectionPort = Objects.requireNonNull(connectionPort);
        this.repositoryStore = Objects.requireNonNull(repositoryStore);
    }

    public ValidatedRepositoryConnection execute(RepositoryConnection connection) {
        ValidatedRepositoryConnection validated = connectionPort.validate(
                Objects.requireNonNull(connection, "connection must not be null"));
        repositoryStore.save(validated);
        return validated;
    }
}
