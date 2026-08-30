package com.codeintel.presentation;

import com.codeintel.application.ports.outbound.RepositoryStore;
import com.codeintel.application.repository.ConnectRepository;
import com.codeintel.application.repository.GetRepositoryConnection;
import com.codeintel.domain.repository.RepositoryId;
import com.codeintel.domain.repository.RepositoryConnection;
import com.codeintel.domain.repository.RepositorySourceType;
import com.codeintel.domain.repository.ValidatedRepositoryConnection;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryConnectionControllerTest {
    @Test
    void connectsAndSelectsRepositoryWithoutReturningAuthorizationMaterial() {
        UUID id = UUID.randomUUID();
        ValidatedRepositoryConnection validated = new ValidatedRepositoryConnection(
                new RepositoryId(id), RepositorySourceType.PUBLIC_GIT_URL,
                "https://github.com/owner/repo.git", Instant.parse("2026-08-30T00:00:00Z"));
        RepositoryStore store = new RepositoryStore() {
            public void save(ValidatedRepositoryConnection connection, RepositoryConnection source) {
            }

            public Optional<ValidatedRepositoryConnection> find(RepositoryId repositoryId) {
                return Optional.of(validated);
            }

            public Optional<RepositoryConnection> findSource(RepositoryId repositoryId) {
                return Optional.empty();
            }
        };
        RepositoryConnectionController controller = new RepositoryConnectionController(
                new ConnectRepository(ignored -> validated, store), new GetRepositoryConnection(store));
        var request = new RepositoryConnectionController.ConnectionRequest(
                RepositorySourceType.PUBLIC_GIT_URL, null, null, null,
                URI.create("https://github.com/owner/repo.git"), null, null, null, null);

        var created = controller.connect(request);
        var selected = controller.get(id);

        assertThat(created).isEqualTo(selected);
        assertThat(created.locator()).doesNotContain("token", "credential");
    }
}
