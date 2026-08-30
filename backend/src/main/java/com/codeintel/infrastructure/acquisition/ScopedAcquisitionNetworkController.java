package com.codeintel.infrastructure.acquisition;

import com.codeintel.domain.acquisition.GitRemoteAcquisitionSource;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ScopedAcquisitionNetworkController implements AcquisitionNetworkController {
    private final AtomicBoolean active = new AtomicBoolean();

    @Override
    public NetworkLease openFor(GitRemoteAcquisitionSource source) {
        if (!active.compareAndSet(false, true)) {
            throw new AcquisitionSafetyException("another acquisition network lease is active");
        }
        return () -> {
            if (!active.compareAndSet(true, false)) {
                throw new AcquisitionSafetyException("acquisition network lease was already closed");
            }
        };
    }
}
