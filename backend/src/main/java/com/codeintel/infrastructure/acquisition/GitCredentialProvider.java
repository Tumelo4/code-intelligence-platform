package com.codeintel.infrastructure.acquisition;

import com.codeintel.domain.acquisition.GitRemoteAcquisitionSource;
import org.eclipse.jgit.transport.CredentialsProvider;

@FunctionalInterface
public interface GitCredentialProvider {
    CredentialLease openFor(GitRemoteAcquisitionSource source);

    interface CredentialLease extends AutoCloseable {
        CredentialsProvider credentials();

        @Override
        void close();
    }
}
