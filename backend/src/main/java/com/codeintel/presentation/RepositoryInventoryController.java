package com.codeintel.presentation;

import com.codeintel.application.inventory.GetRepositoryInventory;
import com.codeintel.application.inventory.InventoryRepository;
import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.inventory.InventoryReport;
import com.codeintel.domain.inventory.MavenProjectDescriptor;
import com.codeintel.domain.inventory.RepositoryInventory;
import com.codeintel.domain.inventory.RepositoryPathInventory;
import com.codeintel.domain.repository.RepositoryId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repository-inventories")
public class RepositoryInventoryController {
    private final InventoryRepository inventoryRepository;
    private final GetRepositoryInventory getRepositoryInventory;

    public RepositoryInventoryController(InventoryRepository inventoryRepository,
            GetRepositoryInventory getRepositoryInventory) {
        this.inventoryRepository = inventoryRepository;
        this.getRepositoryInventory = getRepositoryInventory;
    }

    @PostMapping("/{repositoryId}")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryResponse inventory(@PathVariable UUID repositoryId) {
        return InventoryResponse.from(inventoryRepository.execute(new RepositoryId(repositoryId)));
    }

    @GetMapping("/{repositoryId}")
    public InventoryResponse latest(@PathVariable UUID repositoryId) {
        return InventoryResponse.from(getRepositoryInventory.execute(new RepositoryId(repositoryId)));
    }

    public record InventoryResponse(
            UUID repositoryId, AcquisitionRevision.Kind revisionKind, String revision,
            List<String> languages, List<String> buildSystems, RepositoryPathInventory paths,
            List<MavenProjectDescriptor> mavenProjects, int inspectedFiles, Instant inventoriedAt) {
        static InventoryResponse from(RepositoryInventory inventory) {
            InventoryReport report = inventory.report();
            return new InventoryResponse(inventory.repositoryId().value(),
                    inventory.acquisitionRevision().kind(), inventory.acquisitionRevision().value(),
                    report.languages(), report.buildSystems(), report.paths(), report.mavenProjects(),
                    report.inspectedFiles(), inventory.inventoriedAt());
        }
    }
}
