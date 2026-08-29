# FEATURE-SKILL: Mandatory Codebase Review Skill

## Status

Verified.

## Problem

Repository-controlled execution must not begin unless the platform has loaded the mandatory review and runtime-safety policy at a known immutable revision.

## Goal

Load the canonical Codebase Review Skill from a pre-acquired local checkout, verify its exact Git branch and SHA without executing checkout content, require mandatory references, and persist run provenance.

## Non-Goals

Network acquisition, repository-controlled execution, sandbox execution, analysis, refactoring, and policy interpretation.

## Actors and Preconditions

The analysis orchestrator supplies a local checkout acquired by a trusted platform operation, the canonical repository URI, branch, exact 40-character SHA, and applicable optional references.

## Functional Requirements

- REQ-SKILL-001: Accept only `https://github.com/Tumelo4/codebase-review-skill.git`.
- REQ-SKILL-002: Load `SKILL.md` and `references/runtime-safety.md` for every run.
- REQ-SKILL-003: Load requested optional checklist, large-repository, and report references.
- REQ-SKILL-004: Record skill name, repository, branch, SHA, loaded references, and policy version.
- REQ-SKILL-005: Resolve checkout Git metadata and reject a revision differing from the requested SHA.

## Non-Functional and Security Requirements

Loading is passive UTF-8 filesystem inspection. It launches no checkout process, follows no policy-file symlink, accepts no alternate source, and persists provenance only after complete validation. Failures propagate and therefore block the future execution path.

## Domain and Architecture Impact

The Domain adds immutable skill request, version, reference, SHA, and loaded-policy values. Application adds a load-and-record use case plus outbound loader/store ports. Infrastructure implements filesystem/Git-metadata loading and JDBC provenance persistence. Presentation is unchanged.

## Interfaces and Data

`SkillPort.load(SkillLoadRequest)` returns `LoadedSkill`. `SkillRunProvenanceStore.save(runId, SkillVersion)` records the validated version in `skill_run_provenance`. The policy version equals the pinned commit SHA until the upstream policy exposes a separate immutable version identifier.

## Failure Scenarios

Unregistered source, absent checkout, missing/unsafe file, wrong skill identity, detached/wrong branch, malformed/unavailable Git metadata, and SHA mismatch all fail closed. No provenance row is written after loader failure.

## Observability

Successful run provenance is queryable by run ID. Operational logging and metrics are deferred until an analysis-run workflow exists.

## Acceptance Criteria

- AC-SKILL-001: Given a missing, unsafe, wrong-source, or revision-mismatched policy, loading fails and provenance is not written.
- AC-SKILL-002: Given a valid checkout, mandatory and selected optional references load and the exact SHA is persisted.
- AC-SKILL-003: Architecture tests continue to reject outward layer dependencies.

## Test Plan

Unit-test values and orchestration; filesystem-test canonical source, required references, branch/SHA pinning, and optional references; adapter-test JDBC arguments; run the full Java 21 suite and Compose startup.

## Definition of Done

All requirements trace to passing tests and evidence, architecture remains valid, schema initializes in PostgreSQL, and status becomes Verified.
