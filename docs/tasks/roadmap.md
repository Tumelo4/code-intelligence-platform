# Actionable MVP Roadmap

Every milestone starts only after its feature spec is Approved and ends only after deterministic evidence and traceability are recorded.

| Order | Milestone | Immediate task outcome | State |
|---:|---|---|---|
| 1 | Architecture Foundation | Enforce layers; start backend, frontend, PostgreSQL | Verified |
| 2 | Codebase Review Skill | Pin the mandatory skill SHA, load references, fail closed, persist metadata | Verified |
| 3 | Repository Connection | Validate public Git URL, ZIP, local-dev, and GitHub App contracts | Verified |
| 4 | Safe Acquisition | Acquire an exact commit into immutable original and disposable working copies | Queued |
| 5 | Repository Inventory | Inventory Java/Maven structure without executing repository code | Queued |
| 6 | Static Analysis | Produce normalized metrics and findings through `StaticAnalyzerPort` | Queued |
| 7 | Git Intelligence | Compute history, ownership, churn, and coupling | Queued |
| 8 | Scoring | Calculate health, hotspots, priorities, and eligibility deterministically | Queued |
| 9 | Analysis Dashboard | Expose and render findings, evidence, and prioritization | Queued |
| 10 | Safe Execution Broker | Validate policy and audit every command decision | Queued |
| 11 | Docker Sandbox | Enforce isolation, limits, immutable original, and disabled network | Queued |
| 12 | Baseline Engine | Compile/test original and persist baseline evidence | Queued |
| 13 | LLM Provider | Map provider calls behind `LlmPort`; provide no direct shell | Queued |
| 14 | Refactoring Engine | Patch working copy only and record attempts | Queued |
| 15 | Verification | Compile, test, reanalyze, and deterministically accept/reject | Queued |
| 16 | Characterization Tests | Generate and run tests when baseline coverage is insufficient | Queued |
| 17 | Differential Behavior | Compare selected behaviors and assign confidence | Queued |
| 18 | Retry Agent | Retry bounded failures using deterministic evidence | Queued |
| 19 | Verified Fix UI | Present patch, checks, confidence, failures, and provenance | Queued |
| 20 | Patch Export | Export a reproducible patch and verification evidence | Queued |
| 21 | GitHub PR Integration | Create an authorized reviewable PR; never auto-merge | Queued |

## Completed milestone task breakdown

1. TASK-SKILL-001 — Approve the Codebase Review Skill feature spec with REQ-SKILL-001 through REQ-SKILL-005.
2. TASK-SKILL-002 — Record an ADR for policy acquisition, revision pinning, caching, and fail-closed behavior.
3. TASK-SKILL-003 — Define typed `SkillVersion`, reference, and run-provenance domain contracts.
4. TASK-SKILL-004 — Refine `SkillPort` and implement its infrastructure adapter.
5. TASK-SKILL-005 — Persist skill source, branch, commit SHA, references, and policy version.
6. TASK-SKILL-006 — Test missing/invalid policy failure, SHA pinning, reference loading, and restart behavior.
7. TASK-SKILL-007 — Run architecture/integration tests and store evidence before Milestone 3.

Milestone 4 — Safe Acquisition — is next.
