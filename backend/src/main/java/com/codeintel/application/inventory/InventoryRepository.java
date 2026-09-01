package com.codeintel.application.inventory;

import com.codeintel.application.ports.outbound.AcquisitionRecordStore;
import com.codeintel.application.ports.outbound.RepositoryInventoryPort;
import com.codeintel.application.ports.outbound.RepositoryInventoryStore;
import com.codeintel.domain.inventory.RepositoryInventory;
import com.codeintel.domain.repository.RepositoryId;
import java.time.Clock;

public final class InventoryRepository {
    private final AcquisitionRecordStore acquisitionStore;
    private final RepositoryInventoryPort inventoryPort;
    private final RepositoryInventoryStore inventoryStore;
    private final Clock clock;

    public InventoryRepository(AcquisitionRecordStore acquisitionStore,
            RepositoryInventoryPort inventoryPort, RepositoryInventoryStore inventoryStore,
            Clock clock) {
        this.acquisitionStore = acquisitionStore;
        this.inventoryPort = inventoryPort;
        this.inventoryStore = inventoryStore;
        this.clock = clock;
    }

    public RepositoryInventory execute(RepositoryId repositoryId) {
        var acquisition = acquisitionStore.findLatest(repositoryId)
                .orElseThrow(() -> new InventoryNotFoundException("repository acquisition not found"));
        var inventory = new RepositoryInventory(repositoryId, acquisition.revision(),
                inventoryPort.inspect(acquisition.immutableOriginal()), clock.instant());
        inventoryStore.save(inventory);
        return inventory;
    }
}
