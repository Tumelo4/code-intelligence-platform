package com.codeintel.infrastructure.analysis;

import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.analysis.FindingType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JavaParserStaticAnalyzerAdapterTest {
    @TempDir Path root;

    @Test
    void calculatesMetricsFindingsRangesAndStableIdsDeterministically() throws Exception {
        Path source = root.resolve("src/main/java/example");
        Files.createDirectories(source);
        Files.writeString(source.resolve("Risk.java"), """
                package example;
                import java.util.List;
                class Risk {
                  int a, b;
                  void first(int a, int b) {
                    if (a > 0) {
                      for (int i = 0; i < b; i++) {
                        while (a > i) { a--; }
                      }
                    }
                    a++;
                  }
                  void second(int a, int b) {
                    if (a > 0) {
                      for (int i = 0; i < b; i++) {
                        while (a > i) { a--; }
                      }
                    }
                    a++;
                  }
                }
                """);
        var thresholds = new AnalysisThresholds(3, 5, 1, 2, 2, 1, 5, 1, 1, 4, 10, 10000);
        var adapter = new JavaParserStaticAnalyzerAdapter(thresholds);
        var revision = new AcquisitionRevision(AcquisitionRevision.Kind.GIT_COMMIT, "a".repeat(40));

        var first = adapter.analyze(root, List.of("src/main/java"), revision);
        var second = adapter.analyze(root, List.of("src/main/java"), revision);

        assertThat(first).isEqualTo(second);
        assertThat(first.files()).hasSize(1);
        var file = first.files().getFirst();
        assertThat(file.file()).isEqualTo("src/main/java/example/Risk.java");
        assertThat(file.classCount()).isEqualTo(1);
        assertThat(file.methodCount()).isEqualTo(2);
        assertThat(file.dependencyCount()).isEqualTo(1);
        assertThat(file.classes().getFirst().fieldCount()).isEqualTo(2);
        assertThat(file.classes().getFirst().methods().getFirst().cyclomaticComplexity()).isEqualTo(4);
        assertThat(file.classes().getFirst().methods().getFirst().nestingDepth()).isEqualTo(3);
        assertThat(first.findings()).extracting("type").contains(
                FindingType.LONG_METHOD, FindingType.LARGE_CLASS, FindingType.HIGH_COMPLEXITY,
                FindingType.DEEP_NESTING, FindingType.TOO_MANY_PARAMETERS,
                FindingType.GOD_CLASS, FindingType.DUPLICATED_LOGIC);
        assertThat(first.findings()).allSatisfy(finding -> {
            assertThat(finding.id()).hasSize(24);
            assertThat(finding.file()).doesNotStartWith("/");
            assertThat(finding.range().startLine()).isPositive();
            assertThat(finding.evidence()).isNotBlank();
        });
    }

    @Test
    void rejectsMalformedSourcesAndEscapingRoots() throws Exception {
        Files.createDirectories(root.resolve("src"));
        Files.writeString(root.resolve("src/Broken.java"), "class Broken {");
        var adapter = new JavaParserStaticAnalyzerAdapter(AnalysisThresholds.defaults());
        var revision = new AcquisitionRevision(AcquisitionRevision.Kind.GIT_COMMIT, "b".repeat(40));

        assertThatThrownBy(() -> adapter.analyze(root, List.of("src"), revision))
                .isInstanceOf(StaticAnalysisSafetyException.class).hasMessageContaining("could not be parsed");
        assertThatThrownBy(() -> adapter.analyze(root, List.of("../outside"), revision))
                .isInstanceOf(StaticAnalysisSafetyException.class).hasMessageContaining("root is unsafe");
    }
}
