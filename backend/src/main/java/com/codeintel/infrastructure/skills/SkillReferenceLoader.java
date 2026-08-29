package com.codeintel.infrastructure.skills;

import com.codeintel.domain.skill.SkillReference;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class SkillReferenceLoader {
    public Map<SkillReference, String> load(Path root, Set<SkillReference> references) {
        Map<SkillReference, String> loaded = new EnumMap<>(SkillReference.class);
        for (SkillReference reference : references) {
            try {
                loaded.put(reference, Files.readString(
                        SkillLoader.safeFile(root, reference.path()), StandardCharsets.UTF_8));
            } catch (IOException exception) {
                throw new SkillPolicyUnavailableException("could not read " + reference.path(), exception);
            }
        }
        return Map.copyOf(loaded);
    }
}
