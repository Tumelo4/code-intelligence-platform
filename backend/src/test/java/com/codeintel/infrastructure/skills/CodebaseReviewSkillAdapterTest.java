package com.codeintel.infrastructure.skills;

import com.codeintel.domain.skill.CommitSha;
import com.codeintel.domain.skill.LoadedSkill;
import com.codeintel.domain.skill.SkillLoadRequest;
import com.codeintel.domain.skill.SkillReference;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodebaseReviewSkillAdapterTest {
    private static final String SHA = "0123456789abcdef0123456789abcdef01234567";
    private final CodebaseReviewSkillAdapter adapter = new CodebaseReviewSkillAdapter();

    @TempDir
    Path checkout;

    @Test
    void loadsMandatoryAndSelectedReferencesAtPinnedSha() throws IOException {
        createCheckout(SHA, true);
        Files.writeString(checkout.resolve("references/report-template.md"), "# report");

        LoadedSkill loaded = adapter.load(request(SHA, Set.of(SkillReference.REPORT_TEMPLATE)));

        assertThat(loaded.version().commitSha()).isEqualTo(new CommitSha(SHA));
        assertThat(loaded.version().policyVersion()).isEqualTo(SHA);
        assertThat(loaded.content()).containsKeys(
                SkillReference.SKILL, SkillReference.RUNTIME_SAFETY, SkillReference.REPORT_TEMPLATE);
    }

    @Test
    void blocksWhenPinnedShaDoesNotMatchCheckout() throws IOException {
        createCheckout(SHA, true);

        assertThatThrownBy(() -> adapter.load(request("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", Set.of())))
                .isInstanceOf(SkillPolicyUnavailableException.class)
                .hasMessageContaining("pinned revision");
    }

    @Test
    void blocksWhenRuntimeSafetyReferenceIsMissing() throws IOException {
        createCheckout(SHA, false);

        assertThatThrownBy(() -> adapter.load(request(SHA, Set.of())))
                .isInstanceOf(SkillPolicyUnavailableException.class)
                .hasMessageContaining("runtime-safety.md");
    }

    @Test
    void blocksUnregisteredRepository() throws IOException {
        createCheckout(SHA, true);
        SkillLoadRequest request = new SkillLoadRequest(
                checkout, java.net.URI.create("https://example.com/policy.git"), "main",
                new CommitSha(SHA), Set.of());

        assertThatThrownBy(() -> adapter.load(request))
                .isInstanceOf(SkillPolicyUnavailableException.class)
                .hasMessageContaining("unregistered");
    }

    @Test
    void blocksSkillWithWrongFrontMatterIdentity() throws IOException {
        createCheckout(SHA, true);
        Files.writeString(checkout.resolve("SKILL.md"),
                "---\nname: another-skill\n---\n# name: codebase-review\n");

        assertThatThrownBy(() -> adapter.load(request(SHA, Set.of())))
                .isInstanceOf(SkillPolicyUnavailableException.class)
                .hasMessageContaining("not the codebase-review policy");
    }

    @Test
    void blocksReferenceThatEscapesThroughSymlink() throws IOException {
        createCheckout(SHA, true);
        Path outside = Files.createTempDirectory("outside-skill-policy");
        Files.writeString(outside.resolve("report-template.md"), "# untrusted report");
        Files.delete(checkout.resolve("references/runtime-safety.md"));
        Files.delete(checkout.resolve("references"));
        Files.createSymbolicLink(checkout.resolve("references"), outside);

        assertThatThrownBy(() -> adapter.load(request(SHA, Set.of())))
                .isInstanceOf(SkillPolicyUnavailableException.class)
                .hasMessageContaining("runtime-safety.md");
    }

    private SkillLoadRequest request(String sha, Set<SkillReference> optionalReferences) {
        return new SkillLoadRequest(checkout, SkillRegistry.CODEBASE_REVIEW_REPOSITORY,
                "main", new CommitSha(sha), optionalReferences);
    }

    private void createCheckout(String sha, boolean includeRuntimeSafety) throws IOException {
        Files.createDirectories(checkout.resolve(".git/refs/heads"));
        Files.createDirectories(checkout.resolve("references"));
        Files.writeString(checkout.resolve(".git/HEAD"), "ref: refs/heads/main\n");
        Files.writeString(checkout.resolve(".git/refs/heads/main"), sha + "\n");
        Files.writeString(checkout.resolve("SKILL.md"), "---\nname: codebase-review\n---\n");
        if (includeRuntimeSafety) {
            Files.writeString(checkout.resolve("references/runtime-safety.md"), "# runtime safety");
        }
    }
}
