package com.codeintel.infrastructure.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.analysis.AnalysisFinding;
import com.codeintel.domain.analysis.AnalysisReport;
import com.codeintel.domain.analysis.FindingType;
import com.codeintel.domain.analysis.JavaFileMetrics;
import com.codeintel.domain.analysis.SourceRange;
import com.codeintel.domain.analysis.StaticAnalysisResult;
import com.codeintel.domain.git.AuthorContribution;
import com.codeintel.domain.git.ChangeCoupling;
import com.codeintel.domain.git.FileHistory;
import com.codeintel.domain.git.GitIntelligenceReport;
import com.codeintel.domain.git.GitIntelligenceResult;
import com.codeintel.domain.repository.RepositoryId;
import com.codeintel.domain.scoring.EligibilityReason;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeterministicScoringAdapterTest {
    private final RepositoryId repositoryId = new RepositoryId(UUID.randomUUID());
    private final AcquisitionRevision revision = new AcquisitionRevision(
            AcquisitionRevision.Kind.GIT_COMMIT, "a".repeat(40));

    @Test
    void calculatesDocumentedComponentsHealthPriorityAndEligibility() {
        var analysis = analysis(List.of(file("a.java"), file("b.java")), List.of(
                finding("one", "a.java", "HIGH", "HIGH", FindingType.LONG_METHOD, 2),
                finding("two", "b.java", "LOW", "LOW", FindingType.GOD_CLASS, 1)));
        var git = git(List.of(
                history("a.java", 4, 60, 40, 3), history("b.java", 2, 10, 0, 1)),
                List.of(new ChangeCoupling("a.java", "b.java", 2, 4, 2, 1.0)), false);
        var adapter = new DeterministicScoringAdapter(Set.of(FindingType.LONG_METHOD));

        var report = adapter.score(analysis, git);

        assertThat(report.hotspots()).extracting(value -> value.file()).containsExactly("a.java", "b.java");
        assertThat(report.hotspots().get(0)).satisfies(value -> {
            assertThat(value.staticRisk()).isEqualTo(75);
            assertThat(value.commitActivity()).isEqualTo(100);
            assertThat(value.churn()).isEqualTo(100);
            assertThat(value.ownershipConcentration()).isEqualTo(75);
            assertThat(value.coupling()).isEqualTo(100);
            assertThat(value.score()).isEqualTo(86);
        });
        assertThat(report.hotspots().get(1).score()).isEqualTo(30);
        assertThat(report.healthScore()).isEqualTo(42);
        assertThat(report.priorities()).extracting(value -> value.findingId())
                .containsExactly("one", "two");
        assertThat(report.priorities().get(0).score()).isEqualTo(83);
        assertThat(report.priorities().get(0).eligible()).isTrue();
        assertThat(report.priorities().get(1).reasons()).containsExactly(
                EligibilityReason.LOW_CONFIDENCE, EligibilityReason.LOW_PRIORITY,
                EligibilityReason.UNSUPPORTED_FINDING_TYPE);
    }

    @Test
    void isDeterministicForReorderedInputsAndExplainsMissingMetricsAndTruncation() {
        var firstFinding = finding("b", "missing.java", "MEDIUM", "HIGH",
                FindingType.LARGE_CLASS, 3);
        var secondFinding = finding("a", "a.java", "MEDIUM", "HIGH",
                FindingType.LARGE_CLASS, 3);
        var adapter = new DeterministicScoringAdapter(Set.of(FindingType.LARGE_CLASS));
        var history = history("a.java", 1, 1, 0, 1);

        var first = adapter.score(analysis(List.of(file("a.java")), List.of(firstFinding, secondFinding)),
                git(List.of(history), List.of(), true));
        var second = adapter.score(analysis(List.of(file("a.java")), List.of(secondFinding, firstFinding)),
                git(List.of(history), List.of(), true));

        assertThat(first).isEqualTo(second);
        assertThat(first.priorities()).filteredOn(value -> value.findingId().equals("b"))
                .singleElement().satisfies(value -> assertThat(value.reasons()).containsExactly(
                        EligibilityReason.LOW_PRIORITY, EligibilityReason.MISSING_FILE_METRICS,
                        EligibilityReason.TRUNCATED_HISTORY));
    }

    @Test
    void rejectsRepositoryRevisionDuplicateAndUnknownScaleMismatches() {
        var adapter = new DeterministicScoringAdapter(EnumSet.allOf(FindingType.class));
        var validGit = git(List.of(), List.of(), false);

        assertThatThrownBy(() -> adapter.score(new StaticAnalysisResult(
                new RepositoryId(UUID.randomUUID()), revision, new AnalysisReport(List.of(), List.of()),
                Instant.EPOCH), validGit)).isInstanceOf(ScoringSafetyException.class);
        assertThatThrownBy(() -> adapter.score(analysis(List.of(file("a.java"), file("a.java")), List.of()),
                validGit)).isInstanceOf(ScoringSafetyException.class);
        assertThatThrownBy(() -> adapter.score(analysis(List.of(file("a.java")), List.of(
                finding("id", "a.java", "UNKNOWN", "HIGH", FindingType.LONG_METHOD, 1))),
                validGit)).isInstanceOf(ScoringSafetyException.class);
    }

    private StaticAnalysisResult analysis(List<JavaFileMetrics> files, List<AnalysisFinding> findings) {
        return new StaticAnalysisResult(repositoryId, revision, new AnalysisReport(files, findings), Instant.EPOCH);
    }

    private GitIntelligenceResult git(List<FileHistory> files, List<ChangeCoupling> couplings,
            boolean truncated) {
        return new GitIntelligenceResult(repositoryId, revision,
                new GitIntelligenceReport(List.of(), files, couplings, truncated), Instant.EPOCH);
    }

    private static JavaFileMetrics file(String name) {
        return new JavaFileMetrics(name, 10, 0, 0, 0, List.of());
    }

    private static FileHistory history(String file, int commits, int added, int deleted,
            int authorCommits) {
        return new FileHistory(file, commits, added, deleted, Instant.EPOCH, Instant.EPOCH,
                List.of(new AuthorContribution("c".repeat(64), authorCommits, added, deleted)));
    }

    private static AnalysisFinding finding(String id, String file, String severity, String confidence,
            FindingType type, int line) {
        return new AnalysisFinding(id, id, type, "maintainability", severity, confidence, file,
                new SourceRange(line, line), "evidence", "observation", "rationale",
                "recommendation", "MEDIUM", 50);
    }
}
