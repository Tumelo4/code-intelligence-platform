package com.codeintel.application.inventory;

import com.codeintel.application.ports.outbound.AcquisitionRecordStore;
import com.codeintel.application.ports.outbound.RepositoryInventoryStore;
import com.codeintel.domain.acquisition.AcquiredRepository;
import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.inventory.InventoryReport;
import com.codeintel.domain.inventory.RepositoryInventory;
import com.codeintel.domain.inventory.RepositoryPathInventory;
import com.codeintel.domain.repository.RepositoryId;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryRepositoryTest {
    @Test
    void inventoriesLatestImmutableAcquisitionAndPersistsExactRevision() {
        RepositoryId id = new RepositoryId(UUID.randomUUID());
        AcquisitionRevision revision = new AcquisitionRevision(AcquisitionRevision.Kind.GIT_COMMIT,
                "a".repeat(40));
        Path original = Path.of("/safe/original");
        var acquisition = new AcquiredRepository(id, revision, "HEAD", original,
                Path.of("/safe/working"), 0, Instant.EPOCH);
        CapturingStore store = new CapturingStore();
        Instant now = Instant.parse("2026-09-01T00:00:00Z");
        var useCase = new InventoryRepository(acquisitions(acquisition), path -> {
            assertThat(path).isEqualTo(original);
            return emptyReport();
        }, store, Clock.fixed(now, ZoneOffset.UTC));

        RepositoryInventory result = useCase.execute(id);

        assertThat(result.acquisitionRevision()).isEqualTo(revision);
        assertThat(result.inventoriedAt()).isEqualTo(now);
        assertThat(store.saved).isEqualTo(result);
    }

    @Test
    void failsWhenNoAcquisitionExists() {
        var useCase = new InventoryRepository(acquisitions(null), path -> emptyReport(),
                new CapturingStore(), Clock.systemUTC());

        assertThatThrownBy(() -> useCase.execute(new RepositoryId(UUID.randomUUID())))
                .isInstanceOf(InventoryNotFoundException.class);
    }

    private static AcquisitionRecordStore acquisitions(AcquiredRepository value) {
        return new AcquisitionRecordStore() {
            public void save(AcquiredRepository acquisition) { }
            public Optional<AcquiredRepository> findLatest(RepositoryId id) {
                return Optional.ofNullable(value);
            }
        };
    }

    private static InventoryReport emptyReport() {
        var paths = new RepositoryPathInventory(List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        return new InventoryReport(List.of(), List.of(), paths, List.of(), 0);
    }

    private static final class CapturingStore implements RepositoryInventoryStore {
        private RepositoryInventory saved;
        public void save(RepositoryInventory inventory) { saved = inventory; }
        public Optional<RepositoryInventory> findLatest(RepositoryId id) { return Optional.ofNullable(saved); }
    }
}
