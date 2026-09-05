# ADR-008 — Use Explainable Integer-Weighted Scoring

## Status

Accepted.

## Decision

Milestone 8 derives scoring exclusively from persisted static-analysis and Git-intelligence results linked to the same exact acquisition revision. It uses published integer component scales, fixed weights, decimal half-up rounding, stable tie-breakers, and explicit eligibility reason codes. Normalization denominators come from the complete analyzed file set and zero-valued dimensions remain visible.

Scoring is a pure calculation behind an application port. It does not read the acquisition filesystem, contact a remote, execute a process, expose pseudonymous author IDs, or infer undocumented business importance. A truncated history is represented as an eligibility constraint rather than silently treated as complete evidence.

## Consequences

Scores are reproducible, auditable, and straightforward to test. They express prioritization policy rather than statistical probability. Changing weights, thresholds, supported finding types, or rounding is therefore a versioned product decision that requires specification and evidence updates.
