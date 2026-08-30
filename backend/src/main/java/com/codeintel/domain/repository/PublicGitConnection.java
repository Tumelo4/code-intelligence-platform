package com.codeintel.domain.repository;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;

public record PublicGitConnection(URI repositoryUri) implements RepositoryConnection {
    public PublicGitConnection {
        Objects.requireNonNull(repositoryUri, "repositoryUri must not be null");
        String scheme = repositoryUri.getScheme();
        if (scheme == null || !scheme.toLowerCase(Locale.ROOT).equals("https")
                || repositoryUri.getHost() == null || repositoryUri.getUserInfo() != null
                || (repositoryUri.getPort() != -1 && repositoryUri.getPort() != 443)
                || repositoryUri.getQuery() != null || repositoryUri.getFragment() != null
                || repositoryUri.getPath() == null || repositoryUri.getPath().equals("/")
                || repositoryUri.getPath().contains("/../")) {
            throw new IllegalArgumentException(
                    "public repository URI must be credential-free HTTPS without query or fragment");
        }
    }

    @Override
    public RepositorySourceType sourceType() {
        return RepositorySourceType.PUBLIC_GIT_URL;
    }

    @Override
    public String safeLocator() {
        return repositoryUri.toASCIIString();
    }
}
