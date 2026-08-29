package com.codeintel.application.ports.outbound;

import com.codeintel.domain.skill.SkillVersion;
import java.util.UUID;

public interface SkillRunProvenanceStore {
    void save(UUID runId, SkillVersion version);
}
