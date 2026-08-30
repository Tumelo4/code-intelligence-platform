package com.codeintel.application.acquisition;

import com.codeintel.application.ports.outbound.AcquisitionRecordStore;
import com.codeintel.application.ports.outbound.RepositoryStore;
import com.codeintel.domain.acquisition.AcquiredRepository;
import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.acquisition.GitRemoteAcquisitionSource;
import com.codeintel.domain.repository.PublicGitConnection;
import com.codeintel.domain.repository.RepositoryConnection;
import com.codeintel.domain.repository.RepositoryId;
import com.codeintel.domain.repository.RepositorySourceType;
import com.codeintel.domain.repository.ValidatedRepositoryConnection;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcquireRepositoryTest {
    @Test
    void resolvesPersistedSourceDispatchesAndRecordsExactRevision() {
        RepositoryId id = new RepositoryId(UUID.randomUUID());
        PublicGitConnection connection = new PublicGitConnection(
                URI.create("https://github.com/owner/repo.git"));
        ValidatedRepositoryConnection validated = new ValidatedRepositoryConnection(id,
                RepositorySourceType.PUBLIC_GIT_URL, connection.safeLocator(), Instant.now());
        RepositoryStore repositories = new RepositoryStore() {
            public void save(ValidatedRepositoryConnection ignored, RepositoryConnection source) { }
            public Optional<ValidatedRepositoryConnection> find(RepositoryId ignored) {
                return Optional.of(validated);
            }
            public Optional<RepositoryConnection> findSource(RepositoryId ignored) {
                return Optional.of(connection);
            }
        };
        AcquiredRepository acquired = new AcquiredRepository(id,
                new AcquisitionRevision(AcquisitionRevision.Kind.GIT_COMMIT, "a".repeat(40)),
                "main", Path.of("/tmp/original"), Path.of("/tmp/working"), 0, Instant.now());
        AtomicReference<AcquiredRepository> recorded = new AtomicReference<>();
        AcquisitionRecordStore records = new AcquisitionRecordStore() {
            public void save(AcquiredRepository value) { recorded.set(value); }
            public Optional<AcquiredRepository> findLatest(RepositoryId ignored) { return Optional.empty(); }
        };
        AcquireRepository useCase = new AcquireRepository(repositories,
                ignored -> GitRemoteAcquisitionSource.publicRemote(connection.repositoryUri()),
                ignored -> acquired, ignored -> { throw new AssertionError(); },
                ignored -> { throw new AssertionError(); }, records);

        assertThat(useCase.execute(id, "main")).isSameAs(acquired);
        assertThat(recorded.get()).isSameAs(acquired);
    }
}
