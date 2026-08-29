package com.codeintel.application.ports.outbound;

import com.codeintel.domain.skill.LoadedSkill;
import com.codeintel.domain.skill.SkillLoadRequest;

public interface SkillPort {
    LoadedSkill load(SkillLoadRequest request);
}
