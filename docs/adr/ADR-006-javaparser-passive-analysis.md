# ADR-006 — Use JavaParser Core Without Symbol Resolution

## Status

Accepted.

## Decision

Milestone 6 uses pinned `javaparser-core` 3.28.2 and reads immutable acquired Java files only. It does not enable symbol solving, dependency downloads, compilation, annotation processing, Maven, or repository commands. Metrics derive solely from AST structure and parser source ranges. Finding thresholds are explicit configuration, and stable IDs hash the exact revision, type, relative file, range, and evidence fingerprint.

## Consequences

Analysis is offline and reproducible for incomplete repositories. Dependency metrics describe syntax-visible dependencies rather than a resolved type graph. Semantic resolution can be added only behind a future separately specified safe boundary.
