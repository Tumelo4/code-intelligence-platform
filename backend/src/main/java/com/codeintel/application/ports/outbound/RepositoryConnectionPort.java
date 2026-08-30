package com.codeintel.application.ports.outbound;

import com.codeintel.domain.repository.RepositoryConnection;
import com.codeintel.domain.repository.ValidatedRepositoryConnection;

public interface RepositoryConnectionPort {
    ValidatedRepositoryConnection validate(RepositoryConnection connection);
}
