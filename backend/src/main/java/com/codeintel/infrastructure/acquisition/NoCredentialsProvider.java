package com.codeintel.infrastructure.acquisition;

import com.codeintel.domain.acquisition.GitRemoteAcquisitionSource;

public final class NoCredentialsProvider implements GitCredentialProvider {
    @Override
    public CredentialLease openFor(GitRemoteAcquisitionSource source) {
        if (source.gitHubInstallationId().isPresent()) {
            throw new AcquisitionSafetyException("private Git acquisition credentials are unavailable");
        }
        return new CredentialLease() {
            public org.eclipse.jgit.transport.CredentialsProvider credentials() {
                return null;
            }

            public void close() {
            }
        };
    }
}
