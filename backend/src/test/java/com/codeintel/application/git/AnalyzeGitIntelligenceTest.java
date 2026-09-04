package com.codeintel.application.git;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeintel.application.ports.outbound.AcquisitionRecordStore;
import com.codeintel.application.ports.outbound.GitIntelligenceStore;
import com.codeintel.domain.acquisition.AcquiredRepository;
import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.git.GitIntelligenceReport;
import com.codeintel.domain.git.GitIntelligenceResult;
import com.codeintel.domain.repository.RepositoryId;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnalyzeGitIntelligenceTest {
    @Test
    void analyzesLatestExactRevisionAndPersistsIt() {
        RepositoryId id = new RepositoryId(UUID.randomUUID());
        var revision = new AcquisitionRevision(AcquisitionRevision.Kind.GIT_COMMIT, "a".repeat(40));
        AcquiredRepository acquisition = acquired(id, revision);
        CapturingStore store = new CapturingStore();
        Instant analyzedAt = Instant.parse("2026-09-04T00:00:00Z");
        var useCase = new AnalyzeGitIntelligence(acquisitions(acquisition), (root, exact) -> {
            assertThat(root).isEqualTo(Path.of("/safe/original"));
            assertThat(exact).isEqualTo(revision);
            return new GitIntelligenceReport(List.of(), List.of(), List.of(), false);
        }, store, Clock.fixed(analyzedAt, ZoneOffset.UTC));

        GitIntelligenceResult result = useCase.execute(id);

        assertThat(result.acquisitionRevision()).isEqualTo(revision);
        assertThat(result.analyzedAt()).isEqualTo(analyzedAt);
        assertThat(store.value).isEqualTo(result);
    }

    @Test
    void rejectsHistorylessAcquisitionBeforeCallingAnalyzer() {
        RepositoryId id = new RepositoryId(UUID.randomUUID());
        var revision = new AcquisitionRevision(AcquisitionRevision.Kind.ARCHIVE_SHA256, "b".repeat(64));
        var useCase = new AnalyzeGitIntelligence(acquisitions(acquired(id, revision)),
                (root, exact) -> { throw new AssertionError("must not analyze"); },
                new CapturingStore(), Clock.systemUTC());

        assertThatThrownBy(() -> useCase.execute(id))
                .isInstanceOf(GitIntelligenceValidationException.class)
                .hasMessageContaining("does not contain Git history");
    }

    private static AcquiredRepository acquired(RepositoryId id, AcquisitionRevision revision) {
        return new AcquiredRepository(id, revision, "main", Path.of("/safe/original"),
                Path.of("/safe/working"), 0, Instant.EPOCH);
    }
    private static AcquisitionRecordStore acquisitions(AcquiredRepository value) {
        return new AcquisitionRecordStore() {
            public void save(AcquiredRepository ignored) { }
            public Optional<AcquiredRepository> findLatest(RepositoryId ignored) { return Optional.of(value); }
        };
    }
    private static final class CapturingStore implements GitIntelligenceStore {
        private GitIntelligenceResult value;
        public void save(GitIntelligenceResult result) { value = result; }
        public Optional<GitIntelligenceResult> findLatest(RepositoryId ignored) {
            return Optional.ofNullable(value);
        }
    }
}
