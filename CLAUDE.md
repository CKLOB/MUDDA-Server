# CLAUDE.md

This file defines the guidelines Claude (including Claude Code) must follow when working on the **Anchor (Location-Locked Time Capsule)** project.

## Project Overview

A message capsule service that can only be unlocked by physically visiting a specific GPS coordinate. One-line pitch: "If no one ever goes there, it stays sealed forever."

- Notion spec: "Location-Locked Time Capsule — Feature Spec" (see for core features, tech stack, architecture points)
- Developer: 하민 (Hamin), 2nd-year student at Gwangju Software Meister High School, owns backend/PM/batch
- Goal: portfolio piece + production-ready quality

## Tech Stack

| Area | Technology |
|---|---|
| Language | Kotlin 1.9.x |
| Framework | Spring Boot 3.x (Jakarta EE, Virtual Threads) |
| Build | Gradle Kotlin DSL |
| Database | PostgreSQL 16 + PostGIS 3.x |
| ORM | Spring Data JPA + Hibernate Spatial |
| Cache | Spring Data Redis + Lettuce |
| Storage | AWS S3 (Presigned URL) |
| Push notifications | Firebase Admin SDK (FCM) |
| Auth | Spring Security + JWT (jjwt) |
| Encryption | Bouncy Castle (AES-256-GCM), Shamir's Secret Sharing (custom implementation) |
| Testing | JUnit5 + MockK + Testcontainers |
| Infra | Docker Compose (VM, blue-green) + Nginx, GitHub Actions CI/CD, GHCR |

## Architectural Principles

- Favor a **domain-based package structure**. Instead of flat layering (controller/service/repository), group by domain (capsule, unlock, social, security, etc.), then layer within each domain.
- For complex domains (capsule lock/unlock logic, encryption, geofencing), apply a hexagonal architecture (ports & adapters) to separate domain logic from external infrastructure (PostGIS queries, S3, FCM).
- Don't over-engineer simple CRUD-style domains (e.g. profile lookup). Practice **right-sized engineering** — strip out complexity that scale doesn't justify.
- The server must never be able to see capsule content in plaintext. The encryption/decryption boundary is managed explicitly in a dedicated security module, not scattered across the domain layer.

## Coding Conventions

- Model domains expressively with `data class` and `sealed class` (e.g. capsule state as `sealed class CapsuleStatus` covering sealed/discovered/expired).
- The `kotlin-jpa` plugin is applied to JPA entities, so don't manually add `open`.
- Null safety: never expose platform types; use nullable only when it carries genuine domain meaning.
- Encapsulate spatial queries (`ST_DWithin`, `ST_Distance`, etc.) in the repository layer — never expose raw SQL/JPQL in the service layer.
- Package structure is fixed as `domain/{domainName}/presentation·application·domain·infrastructure` + `global/`. See `AGENTS.md` for detailed naming rules and the Service `execute()` convention.
- Commit messages: `{keyword}: message` format (add/update/fix/delete/docs/test/merge/init). See `AGENTS.md` for details.

## Security Notes

- Any change to AES-256-GCM key/nonce handling must be verified with an encrypt-decrypt round-trip test.
- Before touching the Shamir's Secret Sharing implementation, always check that a threshold change won't break compatibility with shares already distributed.
- Write location verification (anti GPS-spoofing) logic to minimize client trust -- never trust client-submitted coordinates without server-side re-verification.

## How Claude Should Collaborate

- When designing a new feature, don't jump straight to code -- briefly lay out the tradeoffs first (e.g. Redis cache TTL vs. consistency, hexagonal vs. simple layering) and let 하민 decide.
- Don't unilaterally reverse decisions already made in the spec or PLAN.md. If a change seems warranted, explain why and ask for confirmation first.
- Avoid an overly directive tone. Propose as a collaborator; 하민 makes the final call.
- Documentation/architecture design is usually Claude's job, while actual code generation is often handled by Codex. Claude should focus on producing design docs (AGENTS.md, PLAN.md, etc.) that are concrete enough for Codex to execute directly.

## Related Documents

- `AGENTS.md` -- execution instructions for coding agents (e.g. Codex)
- (planned) `PLAN.md` -- phased development plan
- Notion: Location-Locked Time Capsule feature spec, MSA migration design, encryption design