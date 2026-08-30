package com.codeintel.infrastructure.persistence;

import com.codeintel.domain.repository.RepositoryId;
import com.codeintel.domain.repository.GitHubAppConnection;
import com.codeintel.domain.repository.GitHubRepository;
import com.codeintel.domain.repository.RepositorySourceType;
import com.codeintel.domain.repository.ValidatedRepositoryConnection;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JdbcRepositoryStoreTest {
    @Test
    void savesOnlyCredentialFreeValidatedMetadata() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        JdbcRepositoryStore store = new JdbcRepositoryStore(jdbc);
        Instant validatedAt = Instant.parse("2026-08-30T00:00:00Z");
        ValidatedRepositoryConnection connection = new ValidatedRepositoryConnection(
                new RepositoryId(UUID.randomUUID()), RepositorySourceType.GITHUB_APP,
                "github.com/owner/repo", validatedAt);

        store.save(connection, new GitHubAppConnection(42, new GitHubRepository("owner", "repo")));

        ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(anyString(), arguments.capture());
        assertThat(arguments.getValue()).containsExactly(
                connection.repositoryId().value(), "GITHUB_APP", "github.com/owner/repo",
                Timestamp.from(validatedAt), null, 42L, "owner", "repo", null, null, null, null);
    }
}
