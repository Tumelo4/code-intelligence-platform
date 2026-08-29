package com.codeintel.infrastructure.skills;

import com.codeintel.application.ports.outbound.SkillPort;
import com.codeintel.domain.skill.CommitSha;
import com.codeintel.domain.skill.LoadedSkill;
import com.codeintel.domain.skill.SkillLoadRequest;
import com.codeintel.domain.skill.SkillReference;
import com.codeintel.domain.skill.SkillVersion;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class CodebaseReviewSkillAdapter implements SkillPort {
    private final SkillRegistry registry;
    private final SkillLoader skillLoader;
    private final SkillReferenceLoader referenceLoader;
    private final SkillVersionService versionService;

    public CodebaseReviewSkillAdapter() {
        this(new SkillRegistry(), new SkillLoader(), new SkillReferenceLoader(), new SkillVersionService());
    }

    CodebaseReviewSkillAdapter(
            SkillRegistry registry,
            SkillLoader skillLoader,
            SkillReferenceLoader referenceLoader,
            SkillVersionService versionService
    ) {
        this.registry = registry;
        this.skillLoader = skillLoader;
        this.referenceLoader = referenceLoader;
        this.versionService = versionService;
    }

    @Override
    public LoadedSkill load(SkillLoadRequest request) {
        Path root = registry.requireRegisteredCheckout(request.repository(), request.checkoutRoot());
        CommitSha actualSha = versionService.resolveHead(root, request.branch());
        if (!actualSha.equals(request.commitSha())) {
            throw new SkillPolicyUnavailableException("skill checkout SHA does not match pinned revision");
        }

        Set<SkillReference> references = EnumSet.of(
                SkillReference.SKILL, SkillReference.RUNTIME_SAFETY);
        references.addAll(request.optionalReferences());
        Map<SkillReference, String> content = new java.util.EnumMap<>(SkillReference.class);
        content.put(SkillReference.SKILL, skillLoader.load(root));
        content.putAll(referenceLoader.load(root, references.stream()
                .filter(reference -> reference != SkillReference.SKILL)
                .collect(java.util.stream.Collectors.toUnmodifiableSet())));

        SkillVersion version = new SkillVersion(
                "codebase-review",
                request.repository(),
                request.branch(),
                actualSha,
                references,
                actualSha.value());
        return new LoadedSkill(version, content);
    }
}
