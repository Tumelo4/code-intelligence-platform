# ADR-002: Passive Pinned Skill Loading

- Status: Accepted
- Date: 2026-08-29
- Requirements: REQ-SKILL-001 through REQ-SKILL-005

## Context

The runtime-safety policy is itself versioned in Git and must be available before repository-controlled execution. Invoking Git or scripts from the policy checkout during this gate would create an unnecessary execution path before policy validation.

## Decision

Acquisition is a separate trusted-platform responsibility. The loader receives a local checkout, accepts only the canonical repository URI, reads `.git/HEAD` plus loose or packed branch refs, and requires an exact requested SHA. It reads regular non-symlink policy files as UTF-8, always loads the core skill and runtime-safety reference, and loads optional references explicitly. The application persists validated provenance through an outbound port; PostgreSQL stores the initial implementation.

## Consequences

Loading is deterministic and fail-closed without executing checkout content. Detached checkouts are rejected in the MVP. Git worktrees and alternate Git directories are not supported yet. Acquisition and signature/trust verification remain separate future concerns.
