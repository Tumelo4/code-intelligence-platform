package com.codeintel.application.acquisition;

import com.codeintel.application.ports.outbound.AcquisitionRecordStore;
import com.codeintel.domain.acquisition.AcquiredRepository;
import com.codeintel.domain.repository.RepositoryId;
import java.util.Objects;

public final class GetLatestAcquisition {
    private final AcquisitionRecordStore store;

    public GetLatestAcquisition(AcquisitionRecordStore store) {
        this.store = Objects.requireNonNull(store);
    }

    public AcquiredRepository execute(RepositoryId repositoryId) {
        return store.findLatest(repositoryId)
                .orElseThrow(() -> new AcquisitionNotFoundException("repository acquisition not found"));
    }
}
