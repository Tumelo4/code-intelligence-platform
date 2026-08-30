package com.codeintel.application.ports.outbound;

import com.codeintel.domain.acquisition.AcquisitionSource;
import com.codeintel.domain.repository.RepositoryConnection;

public interface AcquisitionSourcePort {
    AcquisitionSource resolve(RepositoryConnection connection);
}
