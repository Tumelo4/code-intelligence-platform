package com.codeintel.presentation;

import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.analysis.AnalysisReport;
import com.codeintel.domain.analysis.StaticAnalysisResult;
import com.codeintel.domain.repository.RepositoryId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StaticAnalysisControllerTest {
    @Test
    void responseContainsRevisionEvidenceWithoutFilesystemPaths() {
        var result = new StaticAnalysisResult(new RepositoryId(UUID.randomUUID()),
                new AcquisitionRevision(AcquisitionRevision.Kind.GIT_COMMIT, "a".repeat(40)),
                new AnalysisReport(List.of(), List.of()), Instant.EPOCH);
        var response = StaticAnalysisController.Response.from(result);
        assertThat(response.revision()).isEqualTo("a".repeat(40));
        assertThat(response.toString()).doesNotContain("/tmp/", "original", "working");
    }
}
