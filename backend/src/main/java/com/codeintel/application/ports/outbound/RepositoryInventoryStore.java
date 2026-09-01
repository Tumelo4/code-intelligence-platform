package com.codeintel.application.ports.outbound;

import com.codeintel.domain.inventory.RepositoryInventory;
import com.codeintel.domain.repository.RepositoryId;
import java.util.Optional;

public interface RepositoryInventoryStore {
    void save(RepositoryInventory inventory);

    Optional<RepositoryInventory> findLatest(RepositoryId repositoryId);
}
