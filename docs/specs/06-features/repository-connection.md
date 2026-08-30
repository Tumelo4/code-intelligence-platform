# FEATURE-REPO: Repository Connection

## Status

Verified.

## Problem

Repository authorization and repository acquisition have different security boundaries. Credentials used to prove access must never become repository metadata or reach the future runtime sandbox.

## Goal

Represent and validate GitHub App, public HTTPS Git, ZIP upload, and local development repository connections before acquisition begins.

## Functional Requirements

- REQ-REPO-001: Keep authorization and access validation separate from repository acquisition.
- REQ-REPO-002: Provide typed contracts for GitHub App, public Git URL, ZIP upload, and local development path sources.
- REQ-REPO-003: Represent private GitHub access by installation ID and repository coordinates, without personal access tokens or credentials in domain values.
- REQ-REPO-004: Return only a generated repository ID, source type, safe locator, and validation timestamp across the connection boundary.

## Security Requirements

Public URLs must be credential-free HTTPS without query or fragment. ZIP metadata must include a safe basename, positive size, and lowercase SHA-256. Local paths must be absolute and resolve to a readable Git directory. Every access probe fails closed. GitHub credentials remain owned by the infrastructure probe and are never accepted or returned by domain/application APIs.

## Acceptance Criteria

- AC-REPO-001: Each MVP source has an immutable validated input contract.
- AC-REPO-002: Invalid or inaccessible sources fail before acquisition.
- AC-REPO-003: GitHub App domain values and validation results contain no credential material.
- AC-REPO-004: Architecture rules remain green.

## Verification

Production GitHub App, controlled-egress public Git, staged ZIP, and opt-in local development probes are wired. Credential-free POST/GET APIs persist validated selections. The Java 21 suite, Compose health, live public Git validation, unsafe URL rejection, persistence, and restart selection paths are recorded in Milestone 3 evidence.
