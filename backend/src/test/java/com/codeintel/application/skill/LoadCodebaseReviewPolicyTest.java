package com.codeintel.application.skill;

import com.codeintel.application.ports.outbound.SkillPort;
import com.codeintel.application.ports.outbound.SkillRunProvenanceStore;
import com.codeintel.domain.skill.CommitSha;
import com.codeintel.domain.skill.LoadedSkill;
import com.codeintel.domain.skill.SkillLoadRequest;
import com.codeintel.domain.skill.SkillReference;
import com.codeintel.domain.skill.SkillVersion;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoadCodebaseReviewPolicyTest {
    private static final CommitSha SHA = new CommitSha("0123456789abcdef0123456789abcdef01234567");
    private static final URI REPOSITORY = URI.create("https://github.com/Tumelo4/codebase-review-skill.git");

    @Test
    void recordsExactLoadedVersionAfterSuccessfulValidation() {
        SkillVersion version = version();
        LoadedSkill loaded = new LoadedSkill(version, Map.of(
                SkillReference.SKILL, "skill", SkillReference.RUNTIME_SAFETY, "safety"));
        AtomicReference<SkillVersion> saved = new AtomicReference<>();
        LoadCodebaseReviewPolicy useCase = new LoadCodebaseReviewPolicy(request -> loaded,
                (runId, savedVersion) -> saved.set(savedVersion));

        useCase.loadForRun(UUID.randomUUID(), request());

        assertThat(saved.get()).isEqualTo(version);
        assertThat(saved.get().commitSha()).isEqualTo(SHA);
    }

    @Test
    void doesNotRecordProvenanceWhenPolicyValidationFails() {
        SkillPort failingPort = request -> { throw new IllegalStateException("invalid policy"); };
        AtomicReference<SkillVersion> saved = new AtomicReference<>();
        SkillRunProvenanceStore store = (runId, version) -> saved.set(version);
        LoadCodebaseReviewPolicy useCase = new LoadCodebaseReviewPolicy(failingPort, store);

        assertThatThrownBy(() -> useCase.loadForRun(UUID.randomUUID(), request()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(saved).hasValue(null);
    }

    private SkillLoadRequest request() {
        return new SkillLoadRequest(Path.of("policy"), REPOSITORY, "main", SHA, Set.of());
    }

    private SkillVersion version() {
        return new SkillVersion("codebase-review", REPOSITORY, "main", SHA,
                Set.of(SkillReference.SKILL, SkillReference.RUNTIME_SAFETY), SHA.value());
    }
}
