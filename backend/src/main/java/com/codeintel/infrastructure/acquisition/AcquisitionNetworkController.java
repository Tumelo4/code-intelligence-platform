package com.codeintel.infrastructure.acquisition;

import com.codeintel.domain.acquisition.GitRemoteAcquisitionSource;

@FunctionalInterface
public interface AcquisitionNetworkController {
    NetworkLease openFor(GitRemoteAcquisitionSource source);

    interface NetworkLease extends AutoCloseable {
        @Override
        void close();
    }
}
