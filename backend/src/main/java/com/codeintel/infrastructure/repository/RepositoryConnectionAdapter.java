package com.codeintel.infrastructure.repository;

import com.codeintel.application.ports.outbound.RepositoryConnectionPort;
import com.codeintel.domain.repository.GitHubAppConnection;
import com.codeintel.domain.repository.LocalDevelopmentConnection;
import com.codeintel.domain.repository.PublicGitConnection;
import com.codeintel.domain.repository.RepositoryConnection;
import com.codeintel.domain.repository.RepositoryId;
import com.codeintel.domain.repository.ValidatedRepositoryConnection;
import com.codeintel.domain.repository.ZipUploadConnection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class RepositoryConnectionAdapter implements RepositoryConnectionPort {
    private final GitHubAppAccessProbe gitHubProbe;
    private final PublicGitAccessProbe publicGitProbe;
    private final ZipUploadAccessProbe zipProbe;
    private final Supplier<UUID> idSupplier;
    private final Clock clock;
    private final boolean localDevelopmentEnabled;

    public RepositoryConnectionAdapter(
            GitHubAppAccessProbe gitHubProbe,
            PublicGitAccessProbe publicGitProbe,
            ZipUploadAccessProbe zipProbe,
            Supplier<UUID> idSupplier,
            Clock clock) {
        this(gitHubProbe, publicGitProbe, zipProbe, idSupplier, clock, true);
    }

    public RepositoryConnectionAdapter(
            GitHubAppAccessProbe gitHubProbe,
            PublicGitAccessProbe publicGitProbe,
            ZipUploadAccessProbe zipProbe,
            Supplier<UUID> idSupplier,
            Clock clock,
            boolean localDevelopmentEnabled) {
        this.gitHubProbe = Objects.requireNonNull(gitHubProbe);
        this.publicGitProbe = Objects.requireNonNull(publicGitProbe);
        this.zipProbe = Objects.requireNonNull(zipProbe);
        this.idSupplier = Objects.requireNonNull(idSupplier);
        this.clock = Objects.requireNonNull(clock);
        this.localDevelopmentEnabled = localDevelopmentEnabled;
    }

    @Override
    public ValidatedRepositoryConnection validate(RepositoryConnection connection) {
        Objects.requireNonNull(connection, "connection must not be null");
        String safeLocator = connection.safeLocator();
        boolean accessible;
        if (connection instanceof GitHubAppConnection github) {
            accessible = gitHubProbe.canRead(github.installationId(), github.repository());
        } else if (connection instanceof PublicGitConnection publicGit) {
            accessible = publicGitProbe.canRead(publicGit.repositoryUri());
        } else if (connection instanceof ZipUploadConnection zip) {
            accessible = zipProbe.isAvailable(zip);
        } else if (connection instanceof LocalDevelopmentConnection local) {
            if (!localDevelopmentEnabled) {
                throw new RepositoryAccessDeniedException("local development connections are disabled");
            }
            safeLocator = validateLocalRepository(local.repositoryPath()).toString();
            accessible = true;
        } else {
            throw new RepositoryAccessDeniedException("unsupported repository source");
        }
        if (!accessible) {
            throw new RepositoryAccessDeniedException(
                    "read access could not be validated for " + connection.sourceType());
        }
        return new ValidatedRepositoryConnection(
                new RepositoryId(idSupplier.get()), connection.sourceType(), safeLocator, clock.instant());
    }

    private static Path validateLocalRepository(Path repositoryPath) {
        try {
            Path realPath = repositoryPath.toRealPath();
            if (!Files.isDirectory(realPath) || !Files.isDirectory(realPath.resolve(".git"))) {
                throw new RepositoryAccessDeniedException("local path is not a readable Git repository");
            }
            return realPath;
        } catch (IOException exception) {
            throw new RepositoryAccessDeniedException("local repository is unavailable", exception);
        }
    }
}
