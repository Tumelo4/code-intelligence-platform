package com.codeintel.domain.inventory;

import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.repository.RepositoryId;
import java.time.Instant;

public record RepositoryInventory(
        RepositoryId repositoryId,
        AcquisitionRevision acquisitionRevision,
        InventoryReport report,
        Instant inventoriedAt) {
    public RepositoryInventory {
        if (repositoryId == null || acquisitionRevision == null || report == null
                || inventoriedAt == null) {
            throw new IllegalArgumentException("repository inventory fields are required");
        }
    }
}
