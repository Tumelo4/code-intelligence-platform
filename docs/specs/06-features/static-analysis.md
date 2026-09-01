# FEATURE-ANALYSIS: Deterministic Java Static Analysis

## Status

Approved — implementation in progress.

## Problem

Repository structure alone cannot identify maintainability risks. Analysis must produce reproducible metrics and evidence without compiling, resolving symbols remotely, or executing repository code.

## Goal

Use pinned JavaParser to compute deterministic Java file, class, and method metrics and emit normalized, evidence-rich findings linked to an exact acquisition revision.

## Non-goals

This milestone does not compile code, resolve external dependencies, execute Maven or repository commands, calculate Git history, score hotspots, or modify files.

## Preconditions

A persisted repository inventory and its matching immutable acquisition revision exist. Analysis reads only normalized Java source roots from that inventory.

## Functional Requirements

- REQ-ANALYSIS-001: Use pinned JavaParser core for passive Java parsing.
- REQ-ANALYSIS-002: Record LOC, class count, method count, method LOC, cyclomatic complexity, nesting depth, parameter count, field count, branch count, loop count, and dependency count with file and line evidence.
- REQ-ANALYSIS-003: Emit LONG_METHOD, LARGE_CLASS, HIGH_COMPLEXITY, DEEP_NESTING, TOO_MANY_PARAMETERS, GOD_CLASS, and DUPLICATED_LOGIC findings through deterministic configurable rules.
- REQ-ANALYSIS-004: Every finding includes a stable ID, title, type, area, severity, confidence, relative file, line range, evidence, observation, rationale, recommendation, effort, and priority.
- REQ-ANALYSIS-005: Persist analysis by repository and exact acquisition revision and expose results without internal filesystem paths.
- REQ-SAFE-004: Static analysis never executes repository-controlled commands.

## Deterministic Metric Rules

Physical LOC is the inclusive parser range for a declaration. Method cyclomatic complexity starts at one and adds one for each `if`, loop, `catch`, conditional expression, switch entry, and each `&&` or `||`. Nesting is the maximum nested control-flow depth. Dependencies are distinct imports plus fully qualified type declarations visible in the AST. Results and evidence are sorted by normalized relative path, line, finding type, and stable ID.

## Default Finding Thresholds

- LONG_METHOD: method LOC greater than 60.
- LARGE_CLASS: class LOC greater than 500 or more than 30 methods.
- HIGH_COMPLEXITY: method cyclomatic complexity greater than 10.
- DEEP_NESTING: method nesting depth greater than 4.
- TOO_MANY_PARAMETERS: method or constructor parameter count greater than 5.
- GOD_CLASS: class LOC greater than 800, more than 40 methods, and more than 15 fields.
- DUPLICATED_LOGIC: normalized statement sequence of at least six statements shared by separate methods. Comments, whitespace, and identifier spelling are excluded from the fingerprint; evidence names both ranges.

## Security and Failure Behavior

Analysis traverses only inventory-declared roots beneath the immutable acquisition tree, rejects symbolic links and escaping paths, enforces file and byte limits, and performs no symbol-solver network access. Parse failures are recorded as deterministic analysis errors and fail the run; absent matching acquisition or inventory returns 404; unsafe input returns 422.

## Acceptance Criteria

- AC-ANALYSIS-001: Repeated analysis of identical bytes and configuration produces identical metrics, findings, evidence, and stable IDs.
- AC-ANALYSIS-002: All required metrics are available with repository-relative files and exact parser line ranges.
- AC-ANALYSIS-003: Every required finding type has a deterministic rule and complete evidence contract.
- AC-ANALYSIS-004: Malformed Java, symlinks, root escapes, and limit violations fail closed without executing code.
- AC-ANALYSIS-005: Revision-linked results persist across restart and API output contains no internal path.

## Test Plan

TEST-ANALYSIS-001 covers metric formulas and ranges. TEST-ANALYSIS-002 covers each threshold finding. TEST-ANALYSIS-003 covers duplicate fingerprints and stable IDs. TEST-ANALYSIS-004 covers malformed sources, symlinks, escapes, and limits. TEST-ANALYSIS-005 covers orchestration and revision matching. TEST-ANALYSIS-006 covers persistence and API mapping. TEST-ANALYSIS-007 repeats analysis to prove determinism. TEST-ANALYSIS-008 records Java 21 and Compose evidence.

## Definition of Done

Requirements, tasks, tests, persistence, API, Java 21 verification, runtime evidence, and traceability are complete.
