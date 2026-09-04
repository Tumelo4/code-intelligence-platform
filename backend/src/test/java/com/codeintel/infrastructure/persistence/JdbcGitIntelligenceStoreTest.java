package com.codeintel.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.git.GitCommit;
import com.codeintel.domain.git.GitIntelligenceReport;
import com.codeintel.domain.git.GitIntelligenceResult;
import com.codeintel.domain.repository.RepositoryId;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcGitIntelligenceStoreTest {
    @Test
    void savesExactRevisionAndOnlyPseudonymousAuthorIdentity() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        var mapper = JsonMapper.builder().findAndAddModules().build();
        var store = new JdbcGitIntelligenceStore(jdbc, mapper);
        String authorId = "d".repeat(64);
        var result = new GitIntelligenceResult(new RepositoryId(UUID.randomUUID()),
                new AcquisitionRevision(AcquisitionRevision.Kind.GIT_COMMIT, "a".repeat(40)),
                new GitIntelligenceReport(List.of(new GitCommit("a".repeat(40), Instant.EPOCH,
                        authorId, List.of("README.md"))), List.of(), List.of(), false), Instant.EPOCH);

        store.save(result);

        ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(anyString(), arguments.capture());
        assertThat(arguments.getValue()).contains(result.repositoryId().value(), "GIT_COMMIT",
                "a".repeat(40));
        assertThat(arguments.getValue()[3].toString()).contains(authorId)
                .doesNotContain("@", "name", "email", "/tmp/");
    }
}
