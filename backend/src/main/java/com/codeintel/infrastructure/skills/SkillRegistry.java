package com.codeintel.infrastructure.skills;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SkillRegistry {
    public static final URI CODEBASE_REVIEW_REPOSITORY =
            URI.create("https://github.com/Tumelo4/codebase-review-skill.git");

    public Path requireRegisteredCheckout(URI repository, Path checkoutRoot) {
        if (!CODEBASE_REVIEW_REPOSITORY.equals(repository)) {
            throw new SkillPolicyUnavailableException("unregistered skill policy repository");
        }
        Path normalized = checkoutRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalized) || !Files.isDirectory(normalized.resolve(".git"))) {
            throw new SkillPolicyUnavailableException("skill checkout or Git metadata is unavailable");
        }
        return normalized;
    }
}
