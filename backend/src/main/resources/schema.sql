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

CREATE TABLE IF NOT EXISTS repository_connection (
    repository_id UUID PRIMARY KEY,
    source_type TEXT NOT NULL,
    safe_locator TEXT NOT NULL,
    validated_at TIMESTAMPTZ NOT NULL,
    repository_uri TEXT,
    github_installation_id BIGINT,
    github_owner TEXT,
    github_name TEXT,
    zip_filename TEXT,
    zip_size BIGINT,
    zip_sha256 CHAR(64),
    local_path TEXT,
    CONSTRAINT repository_connection_source_type
        CHECK (source_type IN ('GITHUB_APP', 'PUBLIC_GIT_URL', 'ZIP_UPLOAD', 'LOCAL_DEVELOPMENT_PATH'))
);

ALTER TABLE repository_connection ADD COLUMN IF NOT EXISTS repository_uri TEXT;
ALTER TABLE repository_connection ADD COLUMN IF NOT EXISTS github_installation_id BIGINT;
ALTER TABLE repository_connection ADD COLUMN IF NOT EXISTS github_owner TEXT;
ALTER TABLE repository_connection ADD COLUMN IF NOT EXISTS github_name TEXT;
ALTER TABLE repository_connection ADD COLUMN IF NOT EXISTS zip_filename TEXT;
ALTER TABLE repository_connection ADD COLUMN IF NOT EXISTS zip_size BIGINT;
ALTER TABLE repository_connection ADD COLUMN IF NOT EXISTS zip_sha256 CHAR(64);
ALTER TABLE repository_connection ADD COLUMN IF NOT EXISTS local_path TEXT;

CREATE TABLE IF NOT EXISTS repository_acquisition (
    repository_id UUID NOT NULL REFERENCES repository_connection(repository_id),
    revision_kind TEXT NOT NULL,
    revision_value TEXT NOT NULL,
    requested_revision TEXT NOT NULL,
    immutable_original TEXT NOT NULL,
    working_copy TEXT NOT NULL,
    skipped_submodules INTEGER NOT NULL,
    acquired_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (repository_id, acquired_at),
    CONSTRAINT repository_acquisition_revision_kind
        CHECK (revision_kind IN ('GIT_COMMIT', 'ARCHIVE_SHA256', 'LOCAL_SNAPSHOT_SHA256')),
    CONSTRAINT repository_acquisition_submodules CHECK (skipped_submodules >= 0)
);
