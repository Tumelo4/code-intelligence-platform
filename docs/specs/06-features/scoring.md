# FEATURE-SCORING: Deterministic Repository Scoring

## Status

Approved — implementation in progress.

## Problem

Static findings and Git history are individually useful but do not provide a stable repository-health summary, hotspot ranking, or an explainable decision about which findings are ready for automated refactoring.

## Goal

Combine persisted static-analysis and Git-intelligence results for the same exact acquisition revision into deterministic health, file-hotspot, finding-priority, and eligibility scores.

## Non-goals

This milestone does not modify files, execute repository code, call an LLM, infer business criticality, consume issue trackers, or accept or reject a refactoring.

## Preconditions

Persisted static-analysis and Git-intelligence results exist for the repository and reference the same exact acquisition revision.

## Functional Requirements

- REQ-SCORE-001: Reject missing inputs and any repository or exact-revision mismatch.
- REQ-SCORE-002: Calculate an integer 0–100 hotspot score for every statically analyzed file from finding risk, commit activity, churn, ownership concentration, and coupling.
- REQ-SCORE-003: Calculate an integer 0–100 repository health score from the highest-risk file hotspots.
- REQ-SCORE-004: Rank findings deterministically using their severity, confidence, and file hotspot score.
- REQ-SCORE-005: Emit an explicit eligible flag and stable reason codes for every finding considered for automated refactoring.
- REQ-SCORE-006: Persist scoring by repository and exact revision and expose ordered results without internal paths or author identity data.

## Deterministic Rules

All component scores are integers from 0 through 100. Division uses decimal arithmetic and final weighted values round half up. Missing Git history for a file contributes zero rather than removing the file.

Finding risk uses severity points `CRITICAL=100`, `HIGH=75`, `MEDIUM=50`, and `LOW=25`, multiplied by confidence `HIGH=100%`, `MEDIUM=75%`, and `LOW=50%`. A file's static-risk component is the sum of its finding risks capped at 100. Commit activity and churn are normalized against the maximum value among analyzed files. Ownership concentration is the largest author commit count divided by the file commit count. Coupling is the greatest emitted coupling strength for the file.

Hotspot score is `45% static risk + 25% commit activity + 15% churn + 10% ownership concentration + 5% coupling`. Repository health is 100 minus the mean hotspot score of the highest-scoring ten files, or all files when fewer than ten exist; an empty report has health 100.

Finding priority is `70% file hotspot + 30% finding risk`. Findings sort by priority descending, then relative file, source start line, type, and stable finding ID. Files sort by hotspot descending then relative path.

A finding is eligible only when its confidence is `HIGH` or `MEDIUM`, priority is at least 50, its file has a matching static metric, Git history is not truncated, and its type is supported by the future refactoring engine. Every failed condition emits one of `LOW_CONFIDENCE`, `LOW_PRIORITY`, `MISSING_FILE_METRICS`, `TRUNCATED_HISTORY`, or `UNSUPPORTED_FINDING_TYPE`, sorted by enum name. Eligible findings have no rejection reasons.

## Security and Failure Behavior

Scoring reads persisted normalized reports only. It performs no filesystem traversal, network access, process execution, or repository command execution. Unknown severity or confidence values, duplicate finding IDs, invalid ranges, and mismatched revisions fail closed with a validation error.

## Acceptance Criteria

- AC-SCORE-001: Repeated scoring of identical ordered or differently ordered inputs produces byte-equivalent results.
- AC-SCORE-002: Component normalization, weights, caps, and half-up rounding match the documented formulas.
- AC-SCORE-003: Health, hotspot, and priority ordering use documented stable tie-breakers.
- AC-SCORE-004: Eligibility is explainable through complete deterministic reason codes.
- AC-SCORE-005: Missing, mismatched, malformed, and truncated inputs fail closed or produce the documented ineligibility reason.
- AC-SCORE-006: Exact-revision results persist across restart and API output contains no internal path or author identifier.

## Test Plan

TEST-SCORE-001 covers weights, normalization, caps, and rounding. TEST-SCORE-002 covers health and hotspot ranking. TEST-SCORE-003 covers finding priority and tie-breakers. TEST-SCORE-004 covers every eligibility reason and supported types. TEST-SCORE-005 covers revision mismatch, malformed input, duplicate IDs, and determinism under reordered inputs. TEST-SCORE-006 covers orchestration, persistence, and API mapping. TEST-SCORE-007 records Java 21 and Compose runtime evidence.

## Definition of Done

Requirements, tasks, contracts, scoring implementation, persistence, API, tests, Java 21 verification, runtime evidence, and traceability are complete.
