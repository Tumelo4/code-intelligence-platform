# Code Intelligence Platform

A spec-driven platform for understanding, safely refactoring, and deterministically verifying Java/Maven repositories.

## Milestone 1

The Java 21/Spring Boot backend is the platform runtime, not a restriction on the languages it will analyze. The verified capabilities began with Java/Maven inventory and Java static findings; Git history is file-language-neutral. The current direction is polyglot: broad passive language discovery, then universal file-level evidence and language-specific analysis adapters. See [the polyglot analysis specification](docs/specs/06-features/polyglot-analysis.md). Sandbox execution, LLM integration, and refactoring remain later milestones.

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
