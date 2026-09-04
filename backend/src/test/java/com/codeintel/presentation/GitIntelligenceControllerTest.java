package com.codeintel.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.git.GitCommit;
import com.codeintel.domain.git.GitIntelligenceReport;
import com.codeintel.domain.git.GitIntelligenceResult;
import com.codeintel.domain.repository.RepositoryId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GitIntelligenceControllerTest {
    @Test
    void responseContainsRevisionAndPseudonymousHistoryWithoutPathsOrContactData() {
        String authorId = "c".repeat(64);
        var report = new GitIntelligenceReport(List.of(new GitCommit("a".repeat(40), Instant.EPOCH,
                authorId, List.of("src/Main.java"))), List.of(), List.of(), false);
        var result = new GitIntelligenceResult(new RepositoryId(UUID.randomUUID()),
                new AcquisitionRevision(AcquisitionRevision.Kind.GIT_COMMIT, "a".repeat(40)),
                report, Instant.EPOCH);

        var response = GitIntelligenceController.Response.from(result);

        assertThat(response.revision()).isEqualTo("a".repeat(40));
        assertThat(response.commits()).singleElement().satisfies(commit ->
                assertThat(commit.authorId()).isEqualTo(authorId));
        assertThat(response.toString()).doesNotContain("@", "/tmp/", "original", "working");
    }
}
