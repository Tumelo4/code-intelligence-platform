# FEATURE-ACQUIRE: Safe Repository Acquisition

## Status

Verified.

## Goal

Acquire an exact repository revision without executing repository-controlled hooks, filters, LFS, submodules, or commands; produce a read-only original and separate writable working copy; record immutable revision evidence.

## Requirements

- REQ-ACQUIRE-001: Remote Git acquisition resolves and records an exact 40-character commit SHA.
- REQ-ACQUIRE-002: Network and credentials are scoped to bare object transfer and closed before tree materialization.
- REQ-ACQUIRE-003: Materialize Git blobs directly without checkout, hooks, recursive submodules, or automatic LFS.
- REQ-ACQUIRE-004: Reject symbolic links, unsafe paths, duplicate targets, excessive files, excessive per-file size, and excessive expanded size.
- REQ-ACQUIRE-005: Produce distinct immutable-original and writable-working directories without Git acquisition metadata.
- REQ-SAFE-004: Repository-controlled commands must never execute outside an approved sandbox.

## Acceptance Criteria

- An exact requested Git revision is materialized and its SHA is returned.
- Credential and network leases are closed before repository content is exposed downstream.
- ZIP traversal, symlink, duplicate, count, and expansion attacks fail closed.
- The original tree is read-only, the working copy is writable, and neither contains `.git` acquisition metadata.

## Verification

Milestone 4 verification is recorded in `docs/evidence/milestone-04-2026-08-30.md`. It covers bounded Git, ZIP, and local-snapshot acquisition; exact revision persistence; scoped GitHub installation credentials; credential-free API responses; immutable and writable tree separation; and restart-safe Compose storage.
