package com.codeintel.application.ports.outbound;

import com.codeintel.domain.acquisition.AcquiredRepository;
import com.codeintel.domain.acquisition.RepositoryAcquisitionRequest;

public interface ArchiveAcquisitionPort {
    AcquiredRepository acquire(RepositoryAcquisitionRequest request);
}
