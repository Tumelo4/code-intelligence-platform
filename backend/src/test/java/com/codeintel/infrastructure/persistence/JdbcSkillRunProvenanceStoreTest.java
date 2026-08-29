package com.codeintel.infrastructure.persistence;

import com.codeintel.domain.skill.CommitSha;
import com.codeintel.domain.skill.SkillReference;
import com.codeintel.domain.skill.SkillVersion;
import java.net.URI;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JdbcSkillRunProvenanceStoreTest {
    @Test
    void persistsExactShaAndAllProvenanceFields() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        JdbcSkillRunProvenanceStore store = new JdbcSkillRunProvenanceStore(jdbcTemplate);
        UUID runId = UUID.randomUUID();
        String sha = "0123456789abcdef0123456789abcdef01234567";
        SkillVersion version = new SkillVersion(
                "codebase-review",
                URI.create("https://github.com/Tumelo4/codebase-review-skill.git"),
                "main",
                new CommitSha(sha),
                Set.of(SkillReference.SKILL, SkillReference.RUNTIME_SAFETY),
                sha);

        store.save(runId, version);

        verify(jdbcTemplate).update(contains("INSERT INTO skill_run_provenance"),
                eq(runId), eq("codebase-review"),
                eq("https://github.com/Tumelo4/codebase-review-skill.git"),
                eq("main"), eq(sha), eq("SKILL.md,references/runtime-safety.md"), eq(sha));
    }
}
