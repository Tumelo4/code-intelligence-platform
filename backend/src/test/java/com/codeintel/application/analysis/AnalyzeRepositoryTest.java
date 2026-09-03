package com.codeintel.application.analysis;

import com.codeintel.application.ports.outbound.AcquisitionRecordStore;
import com.codeintel.application.ports.outbound.RepositoryInventoryStore;
import com.codeintel.application.ports.outbound.StaticAnalysisStore;
import com.codeintel.domain.acquisition.AcquiredRepository;
import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.analysis.AnalysisReport;
import com.codeintel.domain.analysis.StaticAnalysisResult;
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

class AnalyzeRepositoryTest {
    @Test
    void analyzesMatchingRevisionAndPersistsResult() {
        RepositoryId id = new RepositoryId(UUID.randomUUID());
        var revision = new AcquisitionRevision(AcquisitionRevision.Kind.GIT_COMMIT, "a".repeat(40));
        var acquisition = acquired(id, revision);
        var inventory = inventory(id, revision);
        CapturingAnalysisStore store = new CapturingAnalysisStore();
        Instant time = Instant.parse("2026-09-01T00:00:00Z");
        var useCase = new AnalyzeRepository(acquisitions(acquisition), inventories(inventory),
                (root, roots, exact) -> {
                    assertThat(root).isEqualTo(Path.of("/safe/original"));
                    assertThat(roots).containsExactly("src/main/java");
                    assertThat(exact).isEqualTo(revision);
                    return new AnalysisReport(List.of(), List.of());
                }, store, Clock.fixed(time, ZoneOffset.UTC));

        StaticAnalysisResult result = useCase.execute(id);

        assertThat(result.acquisitionRevision()).isEqualTo(revision);
        assertThat(result.analyzedAt()).isEqualTo(time);
        assertThat(store.value).isEqualTo(result);
    }

    @Test
    void rejectsInventoryFromDifferentRevision() {
        RepositoryId id = new RepositoryId(UUID.randomUUID());
        var latest = new AcquisitionRevision(AcquisitionRevision.Kind.GIT_COMMIT, "a".repeat(40));
        var stale = new AcquisitionRevision(AcquisitionRevision.Kind.GIT_COMMIT, "b".repeat(40));
        var useCase = new AnalyzeRepository(acquisitions(acquired(id, latest)), inventories(inventory(id, stale)),
                (root, roots, revision) -> new AnalysisReport(List.of(), List.of()),
                new CapturingAnalysisStore(), Clock.systemUTC());

        assertThatThrownBy(() -> useCase.execute(id)).isInstanceOf(AnalysisValidationException.class)
                .hasMessageContaining("does not match");
    }

    private static AcquiredRepository acquired(RepositoryId id, AcquisitionRevision revision) {
        return new AcquiredRepository(id, revision, "main", Path.of("/safe/original"),
                Path.of("/safe/working"), 0, Instant.EPOCH);
    }
    private static RepositoryInventory inventory(RepositoryId id, AcquisitionRevision revision) {
        var paths = new RepositoryPathInventory(List.of("src/main/java"), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        return new RepositoryInventory(id, revision, new InventoryReport(List.of("JAVA"), List.of("MAVEN"),
                paths, List.of(), 1), Instant.EPOCH);
    }
    private static AcquisitionRecordStore acquisitions(AcquiredRepository value) {
        return new AcquisitionRecordStore() {
            public void save(AcquiredRepository ignored) { }
            public Optional<AcquiredRepository> findLatest(RepositoryId ignored) { return Optional.of(value); }
        };
    }
    private static RepositoryInventoryStore inventories(RepositoryInventory value) {
        return new RepositoryInventoryStore() {
            public void save(RepositoryInventory ignored) { }
            public Optional<RepositoryInventory> findLatest(RepositoryId ignored) { return Optional.of(value); }
        };
    }
    private static final class CapturingAnalysisStore implements StaticAnalysisStore {
        private StaticAnalysisResult value;
        public void save(StaticAnalysisResult result) { value = result; }
        public Optional<StaticAnalysisResult> findLatest(RepositoryId ignored) { return Optional.ofNullable(value); }
    }
}
