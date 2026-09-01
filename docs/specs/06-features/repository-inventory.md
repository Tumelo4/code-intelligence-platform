# FEATURE-INVENTORY: Passive Repository Inventory

## Status

Verified.

## Problem

Downstream analysis needs a deterministic description of repository structure, but Maven model building, plugins, scripts, profiles, and extensions can resolve remote content or execute repository-controlled behavior.

## Goal

Inventory an acquired repository through bounded filesystem inspection and hardened XML parsing only. Detect Java and Maven, source and test roots, Maven modules and declared build metadata, scripts, CI configuration, containers, migrations, documentation, generated or vendored directories, and build output without executing repository code.

## Non-goals

The inventory does not resolve dependencies, interpolate the effective Maven model, download parent POMs or plugins, execute Maven, evaluate scripts, follow symbolic links, or perform source-code metrics.

## Preconditions

- A successful acquisition exists for the repository.
- The immutable original tree is present and readable.
- Its recorded exact revision is retained with the inventory.

## Functional Requirements

- REQ-INVENTORY-001: Detect languages and build systems from deterministic repository evidence.
- REQ-INVENTORY-002: Identify normalized source roots, test roots, manifests, lockfiles, scripts, CI configuration, Docker files, migrations, documentation, generated directories, vendored directories, and build output.
- REQ-INVENTORY-003: For Maven, record POM coordinates, packaging, declared Java version, modules, dependencies, build plugins, plugin executions, Surefire, and Failsafe declarations.
- REQ-INVENTORY-004: Recursively inspect only declared Maven modules that resolve beneath the acquired root and remain within configured file, module, and XML-size limits.
- REQ-INVENTORY-005: Persist one latest inventory per repository and acquisition revision and expose a filesystem-path-free status API.
- REQ-SAFE-004: Never execute repository-controlled commands outside an approved sandbox.

## Domain and Interfaces

`RepositoryInventory` owns repository ID, acquisition revision, detected languages/build systems, categorized paths, Maven project descriptors, and inventory timestamp. `RepositoryInventoryPort` performs passive inspection. `RepositoryInventoryStore` persists and retrieves results. `InventoryRepository` and `GetRepositoryInventory` orchestrate the boundary.

## Security and Failure Behavior

XML external entities, DTDs, XInclude, schema fetching, and entity expansion are disabled. Symbolic links and non-regular manifest files fail closed. Absolute or escaping Maven module paths fail closed. Inventory limits reject excessive files, modules, and manifest bytes. Missing acquisitions return 404; unsafe or unsupported inventory input returns 422. No internal filesystem path is returned by the API.

## Acceptance Criteria

- AC-INVENTORY-001: A Java/Maven repository is detected from files and Maven declarations without running Maven.
- AC-INVENTORY-002: Conventional and declared source/test roots are normalized, deduplicated, and restricted to the acquisition root.
- AC-INVENTORY-003: Maven modules, coordinates, Java version, dependencies, plugins, executions, Surefire, and Failsafe are recorded deterministically.
- AC-INVENTORY-004: Scripts and repository categories are identified with normalized relative paths.
- AC-INVENTORY-005: Malicious XML, symlinks, path escapes, and configured-limit violations fail closed.
- AC-INVENTORY-006: Persisted inventory remains available through the API after backend restart and contains no internal path.

## Test Plan

- TEST-INVENTORY-001: Detect Java/Maven, conventional roots, and repository path categories.
- TEST-INVENTORY-002: Parse a multi-module Maven tree and declared build metadata.
- TEST-INVENTORY-003: Reject DTD/external-entity input and escaping modules.
- TEST-INVENTORY-004: Reject symbolic manifests and file/module/size limit violations.
- TEST-INVENTORY-005: Verify application orchestration and acquisition lookup failures.
- TEST-INVENTORY-006: Verify JDBC persistence and exact acquisition revision linkage.
- TEST-INVENTORY-007: Verify API output excludes internal paths.
- TEST-INVENTORY-008: Run Java 21 and live Compose restart verification.

## Observability

Persist repository ID, exact acquisition revision kind/value, detected stack, module and categorized-path counts, and inventory timestamp. Do not log file contents, credentials, or absolute acquisition paths.

## Definition of Done

All requirements map to tasks and deterministic tests; architecture rules and Java 21 tests pass; Compose runtime and restart behavior are recorded; evidence and traceability are updated.

## Verification

Milestone 5 verification is recorded in `docs/evidence/milestone-05-2026-09-01.md`.
