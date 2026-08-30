package com.codeintel.infrastructure.repository;

import java.net.URI;

@FunctionalInterface
public interface PublicGitAccessProbe {
    boolean canRead(URI repositoryUri);
}
