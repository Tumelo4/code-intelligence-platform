package com.codeintel.application.ports.outbound;

import com.codeintel.domain.repository.RepositoryId;

import java.net.URI;

public interface RepositoryConnectionPort {
    RepositoryId connectPublicRepository(URI repositoryUri);
}
