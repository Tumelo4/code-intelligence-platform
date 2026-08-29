CREATE TABLE IF NOT EXISTS skill_run_provenance (
    run_id UUID PRIMARY KEY,
    skill_name TEXT NOT NULL,
    repository TEXT NOT NULL,
    branch TEXT NOT NULL,
    commit_sha CHAR(40) NOT NULL,
    loaded_references TEXT NOT NULL,
    policy_version TEXT NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT skill_run_provenance_sha_format
        CHECK (commit_sha ~ '^[0-9a-f]{40}$')
);
