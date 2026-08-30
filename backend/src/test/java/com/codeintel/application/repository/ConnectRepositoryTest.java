package com.codeintel.application.repository;

import com.codeintel.application.ports.outbound.RepositoryStore;
import com.codeintel.domain.repository.PublicGitConnection;
import com.codeintel.domain.repository.RepositoryId;
import com.codeintel.domain.repository.RepositorySourceType;
import com.codeintel.domain.repository.ValidatedRepositoryConnection;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectRepositoryTest {
    @Test
    void persistsOnlyValidatedCredentialFreeConnection() {
        PublicGitConnection request = new PublicGitConnection(
                URI.create("https://github.com/owner/repo.git"));
        ValidatedRepositoryConnection validated = new ValidatedRepositoryConnection(
                new RepositoryId(UUID.randomUUID()), RepositorySourceType.PUBLIC_GIT_URL,
                request.safeLocator(), Instant.parse("2026-08-30T00:00:00Z"));
        AtomicReference<ValidatedRepositoryConnection> saved = new AtomicReference<>();
        RepositoryStore store = new RepositoryStore() {
            public void save(ValidatedRepositoryConnection connection) {
                saved.set(connection);
            }

            public Optional<ValidatedRepositoryConnection> find(RepositoryId repositoryId) {
                return Optional.empty();
            }
        };

        ValidatedRepositoryConnection result = new ConnectRepository(ignored -> validated, store)
                .execute(request);

        assertThat(result).isSameAs(validated);
        assertThat(saved.get()).isSameAs(validated);
    }
}
