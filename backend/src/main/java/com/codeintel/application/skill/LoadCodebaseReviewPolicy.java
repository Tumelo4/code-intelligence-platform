package com.codeintel.application.skill;

import com.codeintel.application.ports.outbound.SkillPort;
import com.codeintel.application.ports.outbound.SkillRunProvenanceStore;
import com.codeintel.domain.skill.LoadedSkill;
import com.codeintel.domain.skill.SkillLoadRequest;
import java.util.Objects;
import java.util.UUID;

public final class LoadCodebaseReviewPolicy {
    private final SkillPort skillPort;
    private final SkillRunProvenanceStore provenanceStore;

    public LoadCodebaseReviewPolicy(SkillPort skillPort, SkillRunProvenanceStore provenanceStore) {
        this.skillPort = Objects.requireNonNull(skillPort);
        this.provenanceStore = Objects.requireNonNull(provenanceStore);
    }

    public LoadedSkill loadForRun(UUID runId, SkillLoadRequest request) {
        Objects.requireNonNull(runId, "runId must not be null");
        LoadedSkill loadedSkill = skillPort.load(request);
        provenanceStore.save(runId, loadedSkill.version());
        return loadedSkill;
    }
}
