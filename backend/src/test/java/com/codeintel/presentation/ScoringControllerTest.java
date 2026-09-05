package com.codeintel.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.repository.RepositoryId;
import com.codeintel.domain.scoring.FileHotspot;
import com.codeintel.domain.scoring.ScoringReport;
import com.codeintel.domain.scoring.ScoringResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ScoringControllerTest {
    @Test
    void responseContainsExactRevisionAndScoresWithoutAuthorsOrInternalPaths() {
        var result = new ScoringResult(new RepositoryId(UUID.randomUUID()),
                new AcquisitionRevision(AcquisitionRevision.Kind.GIT_COMMIT, "a".repeat(40)),
                new ScoringReport(40, List.of(new FileHotspot("src/Main.java", 60, 50, 50,
                        50, 50, 50)), List.of()), Instant.EPOCH);

        var response = ScoringController.Response.from(result);

        assertThat(response.revision()).isEqualTo("a".repeat(40));
        assertThat(response.healthScore()).isEqualTo(40);
        assertThat(response.toString()).contains("src/Main.java")
                .doesNotContain("author", "@", "/tmp/", "original", "working");
    }
}
