package com.codeintel.domain.git;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GitIntelligenceContractTest {
    private static final String SHA = "0123456789abcdef0123456789abcdef01234567";

    @Test
    void snapshotsCollectionsAndAcceptsDotsInsideSafeFilenames() {
        var changedFiles = new ArrayList<>(List.of("src/name..part.java"));
        var commit = new GitCommit(SHA, Instant.EPOCH, "a".repeat(64), changedFiles);
        changedFiles.add("later.java");

        assertThat(commit.changedFiles()).containsExactly("src/name..part.java");
        assertThatThrownBy(() -> commit.changedFiles().add("mutation.java"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsEscapingOrPlatformDependentPaths() {
        assertThatThrownBy(() -> new GitCommit(SHA, Instant.EPOCH, "a".repeat(64),
                List.of("../secret"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FileHistory("src\\Main.java", 1, 0, 0,
                Instant.EPOCH, Instant.EPOCH, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void enforcesCanonicalCouplingPairsAndFormulaRange() {
        assertThatThrownBy(() -> new ChangeCoupling("b.java", "a.java", 1, 1, 1, 1.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ChangeCoupling("a.java", "b.java", 2, 1, 2, 1.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ChangeCoupling("a.java", "b.java", 1, 1, 1, 1.01))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
