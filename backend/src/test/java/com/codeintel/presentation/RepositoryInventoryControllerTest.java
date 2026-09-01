package com.codeintel.presentation;

import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.inventory.InventoryReport;
import com.codeintel.domain.inventory.RepositoryInventory;
import com.codeintel.domain.inventory.RepositoryPathInventory;
import com.codeintel.domain.repository.RepositoryId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryInventoryControllerTest {
    @Test
    void mapsInventoryWithoutExposingAcquisitionFilesystemPaths() {
        var paths = new RepositoryPathInventory(List.of("src/main/java"), List.of("src/test/java"),
                List.of("pom.xml"), List.of(), List.of("build.sh"), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of());
        var inventory = new RepositoryInventory(new RepositoryId(UUID.randomUUID()),
                new AcquisitionRevision(AcquisitionRevision.Kind.GIT_COMMIT, "b".repeat(40)),
                new InventoryReport(List.of("JAVA"), List.of("MAVEN"), paths, List.of(), 3),
                Instant.parse("2026-09-01T00:00:00Z"));

        var response = RepositoryInventoryController.InventoryResponse.from(inventory);

        assertThat(response.repositoryId()).isEqualTo(inventory.repositoryId().value());
        assertThat(response.revision()).isEqualTo("b".repeat(40));
        assertThat(response.paths().sourceRoots()).containsExactly("src/main/java");
        assertThat(response.toString()).doesNotContain("/tmp/", "original", "working");
    }
}
