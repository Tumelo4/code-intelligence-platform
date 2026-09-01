package com.codeintel.application.ports.outbound;

import com.codeintel.domain.inventory.InventoryReport;
import java.nio.file.Path;

public interface RepositoryInventoryPort {
    InventoryReport inspect(Path immutableOriginal);
}
