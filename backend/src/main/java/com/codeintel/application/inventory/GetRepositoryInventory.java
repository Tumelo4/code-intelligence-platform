package com.codeintel.application.inventory;

import com.codeintel.application.ports.outbound.RepositoryInventoryStore;
import com.codeintel.domain.inventory.RepositoryInventory;
import com.codeintel.domain.repository.RepositoryId;

public final class GetRepositoryInventory {
    private final RepositoryInventoryStore inventoryStore;

    public GetRepositoryInventory(RepositoryInventoryStore inventoryStore) {
        this.inventoryStore = inventoryStore;
    }

    public RepositoryInventory execute(RepositoryId repositoryId) {
        return inventoryStore.findLatest(repositoryId)
                .orElseThrow(() -> new InventoryNotFoundException("repository inventory not found"));
    }
}
