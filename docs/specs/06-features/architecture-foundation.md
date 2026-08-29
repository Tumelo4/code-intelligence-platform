# FEATURE-ARCH-FOUNDATION: Architecture Foundation

## Status

Verified — 2026-08-29. See `docs/evidence/milestone-01-2026-08-29.md`.

## Problem

The platform needs an executable foundation that prevents later repository-analysis and refactoring features from bypassing architectural boundaries.

## Goal

Provide startable backend, frontend, and PostgreSQL services with test-enforced strict layers.

## Non-Goals

Repository acquisition, analysis, GitHub authentication, sandbox execution, LLM integration, and refactoring.

## Actors

Platform developer and operator.

## Preconditions

Docker Compose is available, or the documented local toolchain is installed.

## Functional Requirements

- REQ-ARCH-001: Backend code uses Domain, Application, Infrastructure, and Presentation layers with inward dependency direction.
- REQ-ARCH-002: Backend capabilities are packaged as a modular monolith.
- REQ-ARCH-003: Automated ArchUnit tests reject forbidden layer dependencies.
- REQ-BASE-001: The Spring Boot backend starts and exposes a health endpoint.
- REQ-BASE-002: The Next.js frontend starts and serves the foundation page.
- REQ-BASE-003: PostgreSQL starts and accepts backend connections.

## Non-Functional Requirements

The backend targets Java 21. Services run as non-root users where custom images are used. Runtime configuration is supplied by environment variables.

## Domain Model Impact

Only `RepositoryId` and immutable `RepositoryRevision` value objects are introduced to make initial port contracts explicit.

## Application Use Cases

None in Milestone 1.

## Ports / Interfaces

RepositoryConnectionPort, GitAcquisitionPort, GitAnalysisPort, StaticAnalyzerPort, SkillPort, SandboxPort, ExecutionPort, LlmPort, and RepositoryStore are declared as outbound contracts. Implementations are deferred.

## Infrastructure Adapters

PostgreSQL runtime connectivity only. Persistence adapters are deferred.

## API Contract

Spring Boot Actuator exposes `GET /actuator/health`. No product REST endpoints exist yet.

## Data Model Changes

None.

## Security Constraints

Repository content remains untrusted. No repository-controlled command path or direct LLM shell path is introduced. Secrets are not embedded in production images; Compose credentials are development-only.

## Failure Scenarios

- An unavailable database makes backend health fail.
- A forbidden layer dependency makes `mvn test` fail.
- A backend health failure prevents the frontend container from starting.

## Observability

Backend health is available through Actuator; Compose health checks cover all services.

## Acceptance Criteria

### AC-ARCH-001

Given production backend classes, when a Domain class depends on Spring or an outer layer, then `mvn test` fails with an architecture violation.

### AC-BASE-001

Given Docker is available, when `docker compose up --build` completes, then PostgreSQL, backend, and frontend report healthy and the frontend responds on port 3000.

## Test Plan

- TEST-ARCH-001: Run ArchUnit rules over all production classes.
- TEST-DOMAIN-001: Reject invalid repository revision SHAs.
- TEST-BASE-001: Run backend Maven tests.
- TEST-BASE-002: Run frontend lint and production build.
- TEST-BASE-003: Build and start Compose; inspect service health.

## Implementation Tasks

See `docs/tasks/milestone-01.md`.

## Definition of Done

All acceptance tests pass, evidence is recorded, traceability is current, and status changes to Verified.
