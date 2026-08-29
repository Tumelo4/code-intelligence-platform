# Code Intelligence Platform

A spec-driven platform for understanding, safely refactoring, and deterministically verifying Java/Maven repositories.

## Milestone 1

This repository currently contains the architecture foundation only: a Java 21/Spring Boot backend, a Next.js frontend, PostgreSQL, enforceable layered-package rules, and the specification/evidence needed to trace the milestone. Analysis, sandbox execution, LLM integration, and refactoring are intentionally not implemented yet.

## Prerequisites

- Docker with Compose
- Or, for local development: Java 21+, Maven 3.9+, Node.js 22+, and PostgreSQL 16

## Run the stack

```bash
docker compose up --build
```

When all health checks pass:

- Frontend: <http://localhost:3000>
- Backend health: <http://localhost:8080/actuator/health>
- PostgreSQL: `localhost:5432` (`codeintel` / `codeintel` for local development only)

Stop it with:

```bash
docker compose down
```

## Verify

```bash
cd backend
mvn test

cd ../frontend
npm install
npm run lint
npm run build
```

Architecture tests are part of `mvn test`; a forbidden dependency causes the build to fail.

## Specifications and work

- [Milestone 1 specification](docs/specs/06-features/architecture-foundation.md)
- [Milestone 1 tasks](docs/tasks/milestone-01.md)
- [Requirement traceability](docs/traceability.md)
- [ADR-001](docs/adr/ADR-001-modular-monolith-layered-architecture.md)
