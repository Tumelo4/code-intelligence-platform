package com.codeintel.infrastructure.git;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeintel.domain.acquisition.AcquisitionRevision;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JGitIntelligenceAdapterTest {
    @TempDir Path temporaryDirectory;

    @Test
    void computesDeterministicHistoryChurnPseudonymousOwnershipAndCoupling() throws Exception {
        Fixture fixture = fixture();
        var adapter = new JGitIntelligenceAdapter(new GitIntelligenceLimits(100, 100, 10_000, 2, 1.0, 100));

        var first = adapter.analyze(fixture.original(), revision(fixture.head()));
        var second = adapter.analyze(fixture.original(), revision(fixture.head()));

        assertThat(first).isEqualTo(second);
        assertThat(first.historyTruncated()).isFalse();
        assertThat(first.commits()).hasSize(2).allSatisfy(commit -> {
            assertThat(commit.authorId()).matches("[0-9a-f]{64}");
            assertThat(commit.toString()).doesNotContain("Alice", "alice@example.com");
            assertThat(commit.changedFiles()).containsExactly("a.txt", "b.txt");
        });
        assertThat(first.files()).extracting(history -> history.file()).containsExactly("a.txt", "b.txt");
        assertThat(first.files()).allSatisfy(history -> {
            assertThat(history.commitCount()).isEqualTo(2);
            assertThat(history.linesAdded()).isEqualTo(2);
            assertThat(history.linesDeleted()).isEqualTo(1);
            assertThat(history.authors()).singleElement().satisfies(author -> {
                assertThat(author.commits()).isEqualTo(2);
                assertThat(author.authorId()).matches("[0-9a-f]{64}");
            });
        });
        assertThat(first.couplings()).singleElement().satisfies(coupling -> {
            assertThat(coupling.firstFile()).isEqualTo("a.txt");
            assertThat(coupling.secondFile()).isEqualTo("b.txt");
            assertThat(coupling.cochangeCount()).isEqualTo(2);
            assertThat(coupling.strength()).isEqualTo(1.0);
        });
    }

    @Test
    void reportsBoundedTraversalAsTruncated() throws Exception {
        Fixture fixture = fixture();
        var adapter = new JGitIntelligenceAdapter(new GitIntelligenceLimits(1, 100, 10_000, 1, 0, 100));

        var report = adapter.analyze(fixture.original(), revision(fixture.head()));

        assertThat(report.commits()).hasSize(1);
        assertThat(report.historyTruncated()).isTrue();
    }

    @Test
    void followsRenamesIntoOneCanonicalFileHistory() throws Exception {
        Path seed = temporaryDirectory.resolve("rename-seed");
        String head;
        try (Git git = Git.init().setDirectory(seed.toFile()).call()) {
            Files.writeString(seed.resolve("old.txt"), "content\n");
            git.add().addFilepattern(".").call();
            git.commit().setMessage("add").setAuthor("Author", "author@example.com")
                    .setCommitter("Author", "author@example.com").call();
            Files.move(seed.resolve("old.txt"), seed.resolve("new.txt"));
            git.add().addFilepattern(".").call();
            git.rm().addFilepattern("old.txt").call();
            head = git.commit().setMessage("rename").setAuthor("Author", "author@example.com")
                    .setCommitter("Author", "author@example.com").call().getId().name();
        }
        Path bundle = temporaryDirectory.resolve("rename-bundle");
        Files.createDirectories(bundle.resolve("original"));
        try (Git ignored = Git.cloneRepository().setURI(seed.toUri().toString())
                .setDirectory(bundle.resolve("history.git").toFile()).setBare(true).call()) {
            var adapter = new JGitIntelligenceAdapter(
                    new GitIntelligenceLimits(100, 100, 10_000, 1, 0, 100));

            var report = adapter.analyze(bundle.resolve("original"), revision(head));

            assertThat(report.files()).singleElement().satisfies(history -> {
                assertThat(history.file()).isEqualTo("new.txt");
                assertThat(history.commitCount()).isEqualTo(2);
            });
        }
    }

    @Test
    void rejectsNonGitAndMissingExactRevisions() throws Exception {
        Fixture fixture = fixture();
        var adapter = new JGitIntelligenceAdapter(new GitIntelligenceLimits(100, 100, 10_000, 1, 0, 100));

        assertThatThrownBy(() -> adapter.analyze(fixture.original(),
                new AcquisitionRevision(AcquisitionRevision.Kind.ARCHIVE_SHA256, "a".repeat(64))))
                .isInstanceOf(GitIntelligenceSafetyException.class);
        assertThatThrownBy(() -> adapter.analyze(fixture.original(), revision("0".repeat(40))))
                .isInstanceOf(GitIntelligenceSafetyException.class);
    }

    @Test
    void failsClosedWhenChangedFileOrDiffByteLimitsAreExceeded() throws Exception {
        Fixture fixture = fixture();

        assertThatThrownBy(() -> new JGitIntelligenceAdapter(
                new GitIntelligenceLimits(100, 1, 10_000, 1, 0, 100))
                .analyze(fixture.original(), revision(fixture.head())))
                .isInstanceOf(GitIntelligenceSafetyException.class)
                .hasMessageContaining("changed-file limit");
        assertThatThrownBy(() -> new JGitIntelligenceAdapter(
                new GitIntelligenceLimits(100, 100, 1, 1, 0, 100))
                .analyze(fixture.original(), revision(fixture.head())))
                .isInstanceOf(GitIntelligenceSafetyException.class)
                .hasMessageContaining("failed safely");
    }

    @Test
    void rejectsSymbolicLinkHistoryAndNormalizesEquivalentAuthorIdentities() throws Exception {
        Fixture fixture = fixture();
        Path unsafeBundle = temporaryDirectory.resolve("unsafe-bundle");
        Path original = Files.createDirectories(unsafeBundle.resolve("original"));
        Files.createSymbolicLink(unsafeBundle.resolve("history.git"),
                fixture.original().resolveSibling("history.git"));
        var adapter = new JGitIntelligenceAdapter(
                new GitIntelligenceLimits(100, 100, 10_000, 1, 0, 100));

        assertThatThrownBy(() -> adapter.analyze(original, revision(fixture.head())))
                .isInstanceOf(GitIntelligenceSafetyException.class)
                .hasMessageContaining("unsafe");

        Path seed = temporaryDirectory.resolve("authors-seed");
        String head;
        try (Git git = Git.init().setDirectory(seed.toFile()).call()) {
            Files.writeString(seed.resolve("owned.txt"), "one\n");
            git.add().addFilepattern(".").call();
            git.commit().setMessage("one").setAuthor(" Alice ", "ALICE@EXAMPLE.COM")
                    .setCommitter("Alice", "alice@example.com").call();
            Files.writeString(seed.resolve("owned.txt"), "two\n");
            git.add().addFilepattern(".").call();
            head = git.commit().setMessage("two").setAuthor("alice", "alice@example.com")
                    .setCommitter("Alice", "alice@example.com").call().getId().name();
        }
        Path authorBundle = temporaryDirectory.resolve("authors-bundle");
        Files.createDirectories(authorBundle.resolve("original"));
        try (Git ignored = Git.cloneRepository().setURI(seed.toUri().toString())
                .setDirectory(authorBundle.resolve("history.git").toFile()).setBare(true).call()) {
            var report = adapter.analyze(authorBundle.resolve("original"), revision(head));

            assertThat(report.files()).singleElement().satisfies(history ->
                    assertThat(history.authors()).singleElement().satisfies(author -> {
                        assertThat(author.commits()).isEqualTo(2);
                        assertThat(author.authorId()).matches("[0-9a-f]{64}");
                    }));
        }
    }

    private Fixture fixture() throws Exception {
        Path seed = temporaryDirectory.resolve("seed");
        PersonIdent author = new PersonIdent("Alice", "alice@example.com");
        String head;
        try (Git git = Git.init().setDirectory(seed.toFile()).call()) {
            Files.writeString(seed.resolve("a.txt"), "one\n");
            Files.writeString(seed.resolve("b.txt"), "one\n");
            git.add().addFilepattern(".").call();
            git.commit().setMessage("initial\nprivate body").setAuthor(author).setCommitter(author).call();
            Files.writeString(seed.resolve("a.txt"), "two\n");
            Files.writeString(seed.resolve("b.txt"), "two\n");
            git.add().addFilepattern(".").call();
            head = git.commit().setMessage("update").setAuthor(author).setCommitter(author)
                    .call().getId().name();
        }
        Path bundle = temporaryDirectory.resolve("bundle");
        Path original = bundle.resolve("original");
        Files.createDirectories(original);
        try (Git ignored = Git.cloneRepository().setURI(seed.toUri().toString())
                .setDirectory(bundle.resolve("history.git").toFile()).setBare(true).call()) {
            return new Fixture(original, head);
        }
    }

    private static AcquisitionRevision revision(String sha) {
        return new AcquisitionRevision(AcquisitionRevision.Kind.GIT_COMMIT, sha);
    }

    private record Fixture(Path original, String head) { }
}
