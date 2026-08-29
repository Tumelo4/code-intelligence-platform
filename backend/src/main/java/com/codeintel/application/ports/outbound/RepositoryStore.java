package com.codeintel.application.ports.outbound;

import com.codeintel.domain.repository.RepositoryId;

public interface RepositoryStore {
    boolean exists(RepositoryId repositoryId);
}
