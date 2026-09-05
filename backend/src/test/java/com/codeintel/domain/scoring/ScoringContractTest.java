package com.codeintel.domain.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.repository.RepositoryId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ScoringContractTest {
    @Test
    void preservesBoundedScoresAndCanonicalEligibilityReasons() {
        var hotspot = new FileHotspot("src/Main.java", 64, 75, 50, 25, 100, 20);
        var priority = new FindingPriority("finding-1", "src/Main.java", 60, 1, false,
                List.of(EligibilityReason.UNSUPPORTED_FINDING_TYPE,
                        EligibilityReason.LOW_CONFIDENCE, EligibilityReason.LOW_CONFIDENCE));
        var report = new ScoringReport(36, List.of(hotspot), List.of(priority));
        var result = new ScoringResult(new RepositoryId(UUID.randomUUID()),
                new AcquisitionRevision(AcquisitionRevision.Kind.GIT_COMMIT, "a".repeat(40)),
                report, Instant.EPOCH);

        assertThat(result.report().hotspots()).containsExactly(hotspot);
        assertThat(priority.reasons()).containsExactly(
                EligibilityReason.LOW_CONFIDENCE, EligibilityReason.UNSUPPORTED_FINDING_TYPE);
    }

    @Test
    void rejectsOutOfRangeScoresUnsafePathsAndInconsistentEligibility() {
        assertThatThrownBy(() -> new FileHotspot("../secret", 0, 0, 0, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FileHotspot("Main.java", 101, 0, 0, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FindingPriority("id", "Main.java", 50, 1, true,
                List.of(EligibilityReason.LOW_PRIORITY)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
