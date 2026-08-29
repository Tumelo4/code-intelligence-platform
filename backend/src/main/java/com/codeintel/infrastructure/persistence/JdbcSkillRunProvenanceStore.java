package com.codeintel.infrastructure.persistence;

import com.codeintel.application.ports.outbound.SkillRunProvenanceStore;
import com.codeintel.domain.skill.SkillReference;
import com.codeintel.domain.skill.SkillVersion;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSkillRunProvenanceStore implements SkillRunProvenanceStore {
    private final JdbcTemplate jdbcTemplate;

    public JdbcSkillRunProvenanceStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(UUID runId, SkillVersion version) {
        String references = version.loadedReferences().stream()
                .sorted(Comparator.comparing(SkillReference::path))
                .map(SkillReference::path)
                .collect(Collectors.joining(","));
        jdbcTemplate.update("""
                INSERT INTO skill_run_provenance
                    (run_id, skill_name, repository, branch, commit_sha, loaded_references, policy_version)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                runId,
                version.name(),
                version.repository().toString(),
                version.branch(),
                version.commitSha().value(),
                references,
                version.policyVersion());
    }
}
