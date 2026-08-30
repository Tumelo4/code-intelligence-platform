# ADR-003 — Separate Repository Authorization and Acquisition

## Status

Accepted.

## Decision

Repository connection validates that a source is readable and returns only credential-free metadata. A later acquisition use case consumes that validated connection through a separate port. GitHub App installation credentials stay inside an infrastructure access probe; repository-controlled processes and domain values never receive them.

All source variants implement a sealed domain contract. The connection adapter delegates remote/blob availability checks to source-specific probes, validates local development repositories passively, fails closed, and creates an opaque repository ID only after access succeeds.

## Consequences

Authorization can evolve independently from JGit/archive acquisition. Tests can prove the credential boundary without live secrets. Production remote probes, connection persistence, and HTTP endpoints remain explicit Milestone 3 work rather than leaking into safe acquisition.
