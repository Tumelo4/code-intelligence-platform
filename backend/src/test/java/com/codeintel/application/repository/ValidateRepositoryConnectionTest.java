package com.codeintel.application.repository;

import com.codeintel.domain.repository.PublicGitConnection;
import com.codeintel.domain.repository.RepositoryId;
import com.codeintel.domain.repository.RepositorySourceType;
import com.codeintel.domain.repository.ValidatedRepositoryConnection;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidateRepositoryConnectionTest {
    @Test
    void delegatesValidationToConnectionBoundary() {
        PublicGitConnection request = new PublicGitConnection(
                URI.create("https://github.com/openai/example.git"));
        ValidatedRepositoryConnection expected = new ValidatedRepositoryConnection(
                new RepositoryId(UUID.randomUUID()), RepositorySourceType.PUBLIC_GIT_URL,
                request.safeLocator(), Instant.parse("2026-08-29T12:00:00Z"));
        ValidateRepositoryConnection useCase = new ValidateRepositoryConnection(ignored -> expected);

        assertThat(useCase.execute(request)).isSameAs(expected);
    }
}
