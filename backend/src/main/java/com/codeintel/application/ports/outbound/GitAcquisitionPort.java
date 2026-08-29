package com.codeintel.application.ports.outbound;

import com.codeintel.domain.repository.RepositoryRevision;
import java.nio.file.Path;

public interface GitAcquisitionPort {
    Path acquire(RepositoryRevision revision);
}
