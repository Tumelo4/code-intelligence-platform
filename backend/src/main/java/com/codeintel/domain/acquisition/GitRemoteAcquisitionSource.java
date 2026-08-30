package com.codeintel.domain.acquisition;

import java.net.URI;
import java.util.Objects;
import java.util.OptionalLong;

public record GitRemoteAcquisitionSource(URI remoteUri, OptionalLong gitHubInstallationId)
        implements AcquisitionSource {
    public GitRemoteAcquisitionSource {
        Objects.requireNonNull(remoteUri, "remoteUri must not be null");
        Objects.requireNonNull(gitHubInstallationId, "gitHubInstallationId must not be null");
        if (gitHubInstallationId.isPresent() && gitHubInstallationId.getAsLong() <= 0) {
            throw new IllegalArgumentException("GitHub installation ID must be positive");
        }
    }

    public static GitRemoteAcquisitionSource publicRemote(URI uri) {
        return new GitRemoteAcquisitionSource(uri, OptionalLong.empty());
    }
}
