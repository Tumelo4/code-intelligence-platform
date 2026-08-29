# ADR-001: Modular Monolith with Strict Layered Architecture

- Status: Accepted
- Date: 2026-08-29
- Requirements: REQ-ARCH-001, REQ-ARCH-002, REQ-ARCH-003

## Context

The platform spans repository acquisition, analysis, sandboxed execution, refactoring, and deterministic verification. These concerns need strong boundaries without the operational cost of distributed services during the MVP.

## Decision

Use one deployable Spring Boot backend organized into Domain, Application, Infrastructure, and Presentation layers. Dependencies point inward: Presentation calls Application; Infrastructure implements Application outbound ports; Application depends on Domain; Domain is framework-independent. JPA models remain separate from domain models. ArchUnit enforces these rules in every backend test run.

The frontend and PostgreSQL are separate runtime processes, but do not turn backend capabilities into microservices.

## Consequences

- Domain behavior remains testable without frameworks.
- External technologies are replaceable behind application ports.
- Cross-layer shortcuts fail the build.
- Package discipline and explicit mapping add some code, accepted in exchange for safety and maintainability.
