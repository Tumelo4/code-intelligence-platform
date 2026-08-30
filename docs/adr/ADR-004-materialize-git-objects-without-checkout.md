# ADR-004 — Materialize Git Objects Without Checkout

## Status

Accepted.

## Decision

Remote Git acquisition uses JGit for a bare object transfer only. After network and credential leases close, the platform resolves the requested commit and writes regular blobs directly from its tree. It does not invoke checkout, hooks, filter processes, Git LFS, submodule recursion, or repository commands. Gitlinks are counted and skipped; symbolic links fail closed.

Archive extraction applies the same path, link, file-count, per-file, and total-expansion limits. Successful acquisition creates a read-only original and a separate writable working copy without `.git` metadata.

## Consequences

Acquisition behavior is deterministic and has a smaller execution surface than a normal clone checkout. Symlink-heavy repositories require an explicit future policy. File mode fidelity is intentionally secondary to safety for analysis inputs.
