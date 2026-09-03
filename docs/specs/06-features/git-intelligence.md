# FEATURE-GIT: Deterministic Git Intelligence

## Status

Approved — implementation in progress.

## Problem

Static structure does not reveal change frequency, concentrated ownership, or files that repeatedly evolve together. Git history must be analyzed reproducibly without network access, repository commands, or exposure of author contact data.

## Goal

Compute deterministic exact-revision commit history, per-file churn, pseudonymous ownership, and change coupling from an acquired Git object database.

## Non-goals

This milestone does not fetch missing history, score hotspots, interpret issue trackers, execute Git or repository-controlled commands, modify files, or analyze non-Git archive and local-snapshot history.

## Preconditions

The latest persisted acquisition is a `GIT_COMMIT` revision and its immutable original contains the matching offline Git object database.

## Functional Requirements

- REQ-GIT-001: Traverse commits reachable from the exact acquired commit using pinned JGit without network access or external processes.
- REQ-GIT-002: Record deterministic commit metadata and repository-relative changed files with rename-aware additions and deletions.
- REQ-GIT-003: Aggregate per-file commit count, additions, deletions, first and last change time, and pseudonymous author contributions.
- REQ-GIT-004: Compute symmetric file change-coupling with explicit minimum support and normalized strength.
- REQ-GIT-005: Enforce configurable commit, changed-file, diff-byte, and coupling-pair limits and report truncated history explicitly.
- REQ-GIT-006: Persist results by repository and exact acquisition revision and expose path-free, personally identifying information-free API output.
- REQ-SAFE-004: Git intelligence never executes repository-controlled commands or contacts a remote.

## Deterministic Rules

Commits are ordered by authored time descending and SHA ascending as a tie-breaker. Files use normalized forward-slash repository-relative paths. Author IDs are lowercase SHA-256 hashes of normalized name and email and raw identities are discarded. File histories sort by path; authors sort by contribution count descending then ID; coupling pairs store lexicographically ordered paths and sort by co-change count descending then paths. Coupling strength equals co-change count divided by the smaller of the two file commit counts.

## Security and Failure Behavior

Analysis opens only the immutable acquisition, disables network transitions, rejects symbolic-link or path escapes, and bounds all history and diff work. A missing object, mismatched revision, invalid repository, or exceeded non-truncatable safety limit fails closed. Non-Git acquisitions return a validation error. Raw author names, emails, internal paths, and commit message bodies are neither persisted nor returned.

## Acceptance Criteria

- AC-GIT-001: Repeated analysis of the same object database, revision, and configuration produces byte-equivalent ordered results.
- AC-GIT-002: Merge, rename, add, modify, and delete history produces correct per-file churn and exact reachable commit membership.
- AC-GIT-003: Ownership uses stable pseudonymous identities and contains no raw author name or email.
- AC-GIT-004: Coupling support, strength, ordering, and pair limits follow the documented formulas.
- AC-GIT-005: Shallow history, non-Git acquisitions, missing objects, path escapes, and configured limits have deterministic fail-closed or explicit truncation behavior.
- AC-GIT-006: Revision-linked results persist across restart and API output contains no internal filesystem paths or personal contact data.

## Test Plan

TEST-GIT-001 covers commit traversal, exact revision, merges, and deterministic ordering. TEST-GIT-002 covers add/modify/delete/rename churn. TEST-GIT-003 covers author normalization and ownership aggregation. TEST-GIT-004 covers coupling formulas, support, strength, and ordering. TEST-GIT-005 covers invalid repositories, mismatches, path safety, shallow history, and limits. TEST-GIT-006 covers orchestration, persistence, and API mapping. TEST-GIT-007 repeats analysis to prove determinism. TEST-GIT-008 records Java 21 and Compose evidence.

## Definition of Done

Requirements, tasks, tests, persistence, API, Java 21 verification, runtime evidence, and traceability are complete.
