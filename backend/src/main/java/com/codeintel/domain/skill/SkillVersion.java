package com.codeintel.domain.skill;

import java.net.URI;
import java.util.Objects;
import java.util.Set;

public record SkillVersion(
        String name,
        URI repository,
        String branch,
        CommitSha commitSha,
        Set<SkillReference> loadedReferences,
        String policyVersion
) {
    public SkillVersion {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        Objects.requireNonNull(repository, "repository must not be null");
        if (branch == null || branch.isBlank()) throw new IllegalArgumentException("branch must not be blank");
        Objects.requireNonNull(commitSha, "commitSha must not be null");
        loadedReferences = Set.copyOf(loadedReferences);
        if (!loadedReferences.contains(SkillReference.SKILL)
                || !loadedReferences.contains(SkillReference.RUNTIME_SAFETY)) {
            throw new IllegalArgumentException("mandatory skill references were not loaded");
        }
        if (policyVersion == null || policyVersion.isBlank()) {
            throw new IllegalArgumentException("policyVersion must not be blank");
        }
    }
}
