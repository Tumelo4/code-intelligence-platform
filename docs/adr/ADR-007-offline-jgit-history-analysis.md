# ADR-007 — Analyze Git History Offline With JGit

## Status

Accepted.

## Decision

Milestone 7 retains the acquisition's bare object database as a read-only `history.git` sibling of the metadata-free original and working trees, then reads it with the pinned JGit dependency. Analysis resolves only the acquired exact commit and its reachable ancestors. It never fetches, executes Git or repository commands, follows replacement objects, or reads refs that are not reachable from the acquired commit. Author identities are normalized to deterministic SHA-256 identifiers before persistence or API output.

History traversal, rename detection, diff size, and co-change calculations use explicit bounded configuration. Files and commits are sorted independently of object iteration order. Coupling strength is the shared commit count divided by the smaller file commit count and is emitted only when configured minimum support and strength are met.

## Consequences

History intelligence is reproducible and works without network access or repository code execution. Shallow or archive acquisitions may provide incomplete or no Git history, which is represented explicitly rather than supplemented from a remote. Identity normalization supports ownership analysis without exposing author names or email addresses.
