# AGENTS.md

This file defines execution instructions for autonomous coding agents (e.g. Codex) working in the **Anchor (Location-Locked Time Capsule)** repo. For design rationale and collaboration principles, see `CLAUDE.md`. This document focuses on "how to actually build/test/commit."

## Package Structure

```
domain
 ├── {domainName}                 # e.g. capsule, unlock, social, security, notification, storage
 │   ├── presentation
 │   │   ├── request
 │   │   ├── response
 │   │   └── controller
 │   ├── application
 │   │   └── impl
 │   ├── domain
 │   │   ├── entity
 │   │   ├── type
 │   │   └── repository
 │   └── infrastructure
 │
global
 ├── common
 ├── config
 ├── security
 ├── exception
 ├── response
 ├── annotation
 ├── util
 └── property
```

Follow this structure exactly when adding a new domain. Do not revert to a layered structure with global `controller/`, `service/`, `repository/` packages.

## Class Naming

| Type | Convention | Example |
| --- | --- | --- |
| Controller | `{Domain}Controller` | `CapsuleController` |
| Service | `{Action}{Domain}Service` | `CreateCapsuleService` |
| Repository | `{Domain}Repository` | `CapsuleRepository` |
| Entity | Singular noun | `Capsule`, `Member`, `Notification` |
| Request DTO | `{Action}{Domain}Request` | `CreateCapsuleRequest` |
| Response DTO | `{Action}{Domain}Response` | `CapsuleDetailResponse` |
| Enum | Noun form | `MediaType`, `CapsuleStatus` |

Request/Response DTOs live under each domain's `presentation/request` and `presentation/response`.

## Service Method Convention

Services are split one-per-action (`{Action}{Domain}Service`), and the core method is always named `execute`.

```kotlin
@Service
class CreateCapsuleService(
    private val capsuleRepository: CapsuleRepository
) {
    fun execute(request: CreateCapsuleRequest, memberId: Long): CreateCapsuleResponse {
        // ...
    }
}
```

If an interface is needed, put the interface in `application` and the implementation in `application/impl`.

## Build & Test Commands

```bash
# Build
./gradlew build

# Unit tests only
./gradlew test

# Including integration tests (Testcontainers, requires Docker)
./gradlew check

# Run a specific test class
./gradlew test --tests "anchor.capsule.*"

# Local dev environment (PostgreSQL+PostGIS, Redis)
docker compose up -d
```

Agents must run `./gradlew test` after any code change and confirm the result. Note that Testcontainers-based integration tests may be skipped in environments without a Docker daemon.

## Code Style

- Follow the official Kotlin style guide (ktlint to be introduced -- until then, use IntelliJ's default formatter as the baseline).
- Keep functions/classes single-responsibility and small. In particular, geofencing verification logic in the `unlock` domain should be extracted as pure functions that are easy to unit test.
- Abstract external API clients (S3, FCM, Kakao Map/Google Maps) behind interfaces with adapter implementations, so they can be swapped out easily with MockK in tests.
- No magic numbers: manage radius, max expiration (10 years), per-user capsule limits, etc. via `application.yml` or a constants object.

## Testing Rules

- Use **MockK** instead of Mockito.
- Use `springmockk`'s `@MockkBean` instead of Spring's `@MockBean`.
- Integration tests spin up real PostgreSQL+PostGIS and Redis containers via Testcontainers. Do not substitute an in-memory DB (e.g. H2) -- PostGIS spatial queries cannot be verified on H2.
- Encryption-related code (AES-256-GCM) must always include an encrypt-then-decrypt round-trip test.
- Test the controller layer with `MockMvc`.

## Git / Commit Convention

Format: `{keyword}: {message}`

| Type | Meaning |
| --- | --- |
| add | Added new code or files |
| update | Modified existing code |
| fix | Fixed a bug |
| delete | Removed something |
| docs | Updated documentation |
| test | Added or modified tests |
| merge | Merged a branch |
| init | Project initialization |

- Example: `add: create capsule API`, `fix: geofence radius calculation bug`
- Branch naming: name clearly by domain, e.g. `feature/capsule-creation`, `fix/geofence-radius-bug`.

## Do Not

- Never log raw capsule content in plaintext.
- Never write code that trusts client-side location verification alone (server-side PostGIS re-verification is mandatory).
- Never modify a Flyway migration file that has already been deployed. Add a new migration file instead.
- Never add a dependency to `build.gradle.kts` without a version (except where managed by a BOM).
- Never change the architecture (domain-based -> layered, etc.) unilaterally.

## CI/CD

### CI -- `ci.yml`
- **Trigger**: PR open / push (all branches)
- **Steps**: `lint + compile (ktlint, Gradle)` -> `unit test (JUnit5, MockK)` -> `integration test (Testcontainers)`
- All three steps (`needs: [lint, unit, integration]`) must pass before proceeding.
- **Docker build + GHCR push**: pushes the image as `ghcr.io/.../anchor:$SHA`. This step runs **only on the develop/main branches**; regular PR branches stop after lint/test.
- Once build/push completes, a `workflow_run` event is emitted to trigger CD.

### CD -- `cd.yml`
- **Trigger**: `workflow_run` (ci.yml completed) -- `develop -> staging`, `main -> prod`

**develop branch (staging)**
1. `SSH -> VM` (`appleboy/ssh-action`)
2. `docker compose up green` -- Flyway migrate -> healthcheck
3. Switch Nginx upstream

**main branch (prod)**
1. `manual approval` -- GitHub Environment `prod` (mandatory manual approval before deploy)
2. `SSH -> VM` -- reuses the same blue-green script as develop
3. Switch Nginx upstream

- Both paths converge on a **Slack / PR comment notification**.
- Since this is a blue-green deployment, only switch the Nginx upstream after the new (green) container passes its healthcheck. Do not tear down the existing (blue) container immediately, in case a rollback is needed.
- Prod deploys must always go through the GitHub Environment manual approval step. Do not bypass this step in any workflow change.
- Flyway migrations run automatically when the green container starts, so any breaking schema change must be split into backward-compatible steps (add-then-backfill-then-drop).

## Related Documents

- `CLAUDE.md` -- collaboration principles, architectural background
- Notion: Location-Locked Time Capsule feature spec, MSA migration design, encryption design