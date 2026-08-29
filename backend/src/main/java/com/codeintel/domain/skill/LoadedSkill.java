package com.codeintel.domain.skill;

import java.util.Map;
import java.util.Objects;

public record LoadedSkill(SkillVersion version, Map<SkillReference, String> content) {
    public LoadedSkill {
        Objects.requireNonNull(version, "version must not be null");
        content = Map.copyOf(content);
        if (!content.keySet().equals(version.loadedReferences())) {
            throw new IllegalArgumentException("content and loaded reference provenance must match");
        }
    }
}
