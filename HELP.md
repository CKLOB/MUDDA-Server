# MUDDA Server

Kotlin + Spring Boot backend server.

## Stack

- Kotlin 1.9.25
- Java 21
- Spring Boot 3.5.16
- Gradle Kotlin DSL
- PostgreSQL / PostGIS
- Redis
- Flyway
- Spring Security
- AWS S3
- Firebase Admin SDK
- JJWT
- springdoc-openapi

## Local Checks

Run tests:

```bash
./gradlew test
```

The default context-load test uses Testcontainers for PostgreSQL, so Docker must be running.

Run locally:

```bash
cp .env.example .env
./gradlew bootRun
```

Spring Boot starts the Docker Compose services for PostGIS and Redis during local runs.
Use `http://localhost:8080/actuator/health` for the local health endpoint.

Check the Compose file:

```bash
docker compose config
```

## Gradle Files

- `build.gradle.kts`: plugins and dependency setup
- `settings.gradle.kts`: root project name
- `gradle/wrapper/gradle-wrapper.properties`: Gradle wrapper version

## Notes

- Text files are normalized to LF through `.gitattributes`.
- `gradlew.bat` is kept as CRLF for Windows compatibility.
- `buildSrc` is intentionally not used while this project is a single module.
