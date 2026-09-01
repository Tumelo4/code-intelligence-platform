# ADR-005 — Parse Maven Metadata Without Model Building

## Status

Accepted.

## Decision

Repository inventory uses bounded filesystem traversal and a hardened JDK XML parser. It reads declared POM values directly and never invokes Maven, Maven Model Builder, wrappers, plugins, extensions, profiles, dependency resolution, parent resolution, scripts, or repository commands. External entities, DTDs, XInclude, and external schema access are disabled.

Declared module and source paths are normalized against the immutable acquisition root and must remain beneath it. Symbolic manifests and escaping module paths fail closed. Unresolved Maven properties are retained as declared text rather than evaluated.

## Consequences

Inventory is passive, deterministic, offline, and suitable for untrusted repositories. It describes declared rather than effective Maven configuration; inherited or profile-derived values may remain unknown until a future sandboxed model-resolution feature is explicitly specified.
