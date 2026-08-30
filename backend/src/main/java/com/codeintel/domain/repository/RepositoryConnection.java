package com.codeintel.domain.repository;

public sealed interface RepositoryConnection permits GitHubAppConnection, PublicGitConnection,
        ZipUploadConnection, LocalDevelopmentConnection {
    RepositorySourceType sourceType();

    String safeLocator();
}
