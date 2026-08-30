package com.codeintel.application.repository;

import com.codeintel.application.ports.outbound.RepositoryConnectionPort;
import com.codeintel.domain.repository.RepositoryConnection;
import com.codeintel.domain.repository.ValidatedRepositoryConnection;
import java.util.Objects;

public final class ValidateRepositoryConnection {
    private final RepositoryConnectionPort connectionPort;

    public ValidateRepositoryConnection(RepositoryConnectionPort connectionPort) {
        this.connectionPort = Objects.requireNonNull(connectionPort, "connectionPort must not be null");
    }

    public ValidatedRepositoryConnection execute(RepositoryConnection connection) {
        return connectionPort.validate(Objects.requireNonNull(connection, "connection must not be null"));
    }
}
