package com.codeintel.application.ports.outbound;

import com.codeintel.domain.acquisition.AcquiredRepository;
import com.codeintel.domain.repository.RepositoryId;
import java.util.Optional;

public interface AcquisitionRecordStore {
    void save(AcquiredRepository acquisition);

    Optional<AcquiredRepository> findLatest(RepositoryId repositoryId);
}
