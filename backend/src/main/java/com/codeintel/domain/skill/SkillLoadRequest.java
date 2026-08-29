package com.codeintel.domain.skill;

import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

public record SkillLoadRequest(
        Path checkoutRoot,
        URI repository,
        String branch,
        CommitSha commitSha,
        Set<SkillReference> optionalReferences
) {
    public SkillLoadRequest {
        Objects.requireNonNull(checkoutRoot, "checkoutRoot must not be null");
        Objects.requireNonNull(repository, "repository must not be null");
        if (branch == null || branch.isBlank()) throw new IllegalArgumentException("branch must not be blank");
        Objects.requireNonNull(commitSha, "commitSha must not be null");
        optionalReferences = Set.copyOf(optionalReferences);
        if (optionalReferences.stream().anyMatch(SkillReference::required)) {
            throw new IllegalArgumentException("optionalReferences must contain optional references only");
        }
    }
}
