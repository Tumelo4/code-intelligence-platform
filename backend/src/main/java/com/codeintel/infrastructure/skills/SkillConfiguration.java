package com.codeintel.infrastructure.skills;

import com.codeintel.application.ports.outbound.SkillPort;
import com.codeintel.application.ports.outbound.SkillRunProvenanceStore;
import com.codeintel.application.skill.LoadCodebaseReviewPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SkillConfiguration {
    @Bean
    SkillPort codebaseReviewSkillPort() {
        return new CodebaseReviewSkillAdapter();
    }

    @Bean
    LoadCodebaseReviewPolicy loadCodebaseReviewPolicy(
            SkillPort skillPort,
            SkillRunProvenanceStore provenanceStore
    ) {
        return new LoadCodebaseReviewPolicy(skillPort, provenanceStore);
    }
}
