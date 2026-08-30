package com.codeintel.infrastructure.acquisition;

import com.codeintel.application.ports.outbound.AcquisitionSourcePort;
import com.codeintel.domain.acquisition.AcquisitionSource;
import com.codeintel.domain.acquisition.GitRemoteAcquisitionSource;
import com.codeintel.domain.acquisition.LocalDirectoryAcquisitionSource;
import com.codeintel.domain.acquisition.ZipArchiveAcquisitionSource;
import com.codeintel.domain.repository.GitHubAppConnection;
import com.codeintel.domain.repository.LocalDevelopmentConnection;
import com.codeintel.domain.repository.PublicGitConnection;
import com.codeintel.domain.repository.RepositoryConnection;
import com.codeintel.domain.repository.ZipUploadConnection;
import java.net.URI;
import java.nio.file.Path;
import java.util.OptionalLong;

public final class RepositoryAcquisitionSourceAdapter implements AcquisitionSourcePort {
    private final Path zipStagingRoot;

    public RepositoryAcquisitionSourceAdapter(Path zipStagingRoot) {
        this.zipStagingRoot = zipStagingRoot.toAbsolutePath().normalize();
    }

    @Override
    public AcquisitionSource resolve(RepositoryConnection connection) {
        if (connection instanceof PublicGitConnection publicGit) {
            return GitRemoteAcquisitionSource.publicRemote(publicGit.repositoryUri());
        }
        if (connection instanceof GitHubAppConnection github) {
            URI uri = URI.create("https://github.com/" + github.repository().fullName() + ".git");
            return new GitRemoteAcquisitionSource(uri, OptionalLong.of(github.installationId()));
        }
        if (connection instanceof ZipUploadConnection zip) {
            return new ZipArchiveAcquisitionSource(
                    zipStagingRoot.resolve(zip.contentSha256() + ".zip"), zip.contentSha256());
        }
        if (connection instanceof LocalDevelopmentConnection local) {
            return new LocalDirectoryAcquisitionSource(local.repositoryPath());
        }
        throw new AcquisitionSafetyException("unsupported repository connection source");
    }
}
