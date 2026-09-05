package com.codeintel.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.repository.RepositoryId;
import com.codeintel.domain.scoring.FileHotspot;
import com.codeintel.domain.scoring.ScoringReport;
import com.codeintel.domain.scoring.ScoringResult;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcScoringStoreTest {
    @Test
    void savesExactRevisionWithoutAuthorIdentityOrInternalPaths() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        var store = new JdbcScoringStore(jdbc, JsonMapper.builder().findAndAddModules().build());
        var result = new ScoringResult(new RepositoryId(UUID.randomUUID()),
                new AcquisitionRevision(AcquisitionRevision.Kind.GIT_COMMIT, "a".repeat(40)),
                new ScoringReport(50, List.of(new FileHotspot("src/Main.java", 50, 50, 50,
                        50, 50, 50)), List.of()), Instant.EPOCH);

        store.save(result);

        ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(anyString(), arguments.capture());
        assertThat(arguments.getValue()).contains(result.repositoryId().value(), "GIT_COMMIT",
                "a".repeat(40));
        assertThat(arguments.getValue()[3].toString()).contains("src/Main.java")
                .doesNotContain("authorId", "@", "/tmp/", "original", "working");
    }
}
