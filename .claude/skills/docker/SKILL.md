---
name: docker
description: Dockerfile and docker-compose authoring guide — multi-stage builds, layer caching, security best practices, and compose service wiring.
---

# Docker Guide

## Dockerfile

### Multi-stage build

```dockerfile
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew dependencies --no-daemon
COPY src src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Layer caching rules

- `COPY` dependency manifests first, run install, then `COPY` source
- Only invalidate layers that actually changed

### Security

- Use specific digest tags, not `latest`
- Run as non-root: `RUN adduser --disabled-password app && USER app`
- Never `COPY . .` before installing dependencies

## docker-compose.yml (MUDDA 실제 패턴)

MUDDA는 PostGIS + Redis 조합을 쓰며, 실제 구성은 `docker-compose.yml`을 그대로 따른다:

```yaml
services:
  postgres:
    image: postgis/postgis:16-3.5-alpine
    platform: linux/amd64  # Apple Silicon 등에서 postgis 공식 이미지가 arm64를 지원하지 않아 명시
    ports:
      - "${DB_PORT:-5432}:5432"
    environment:
      POSTGRES_DB: ${DB_NAME:-mudda}
      POSTGRES_USER: ${DB_USERNAME:-mudda}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-mudda}
    labels:
      org.springframework.boot.service-connection: postgres
    volumes:
      - mudda-postgres-data:/var/lib/postgresql/data
      - ./src/main/resources/db/init/001_enable_postgis.sql:/docker-entrypoint-initdb.d/001_enable_postgis.sql:ro

  redis:
    image: redis:7.4-alpine
    ports:
      - "${REDIS_PORT:-6379}:6379"
    volumes:
      - mudda-redis-data:/data

volumes:
  mudda-postgres-data:
  mudda-redis-data:
```

새 서비스를 추가할 때 지켜야 할 패턴:
- 환경변수는 `${VAR:-default}` 형태로 기본값을 두어 `.env` 없이도 기동 가능하게 함
- DB 컨테이너에 `org.springframework.boot.service-connection` 라벨을 붙이면 Spring Boot가 자동으로 연결 정보를 주입함(수동 `spring.datasource.*` 설정 불필요)
- 볼륨은 `<서비스명>-<용도>-data` 네이밍으로 named volume 사용
- 초기화 SQL이 필요하면 `docker-entrypoint-initdb.d/`에 `:ro`로 마운트

**MUDDA 고유 동작**: `build.gradle.kts`에 `developmentOnly("org.springframework.boot:spring-boot-docker-compose")`가 있어, `./gradlew bootRun` 실행 시 Spring Boot가 `docker-compose.yml`을 자동으로 감지해 기동/종료한다. 별도로 `docker compose up`을 수동 실행할 필요가 없다(단, 통합 테스트는 Testcontainers를 별도로 사용하므로 이 자동 기동과는 무관).

## .dockerignore

```
.git
build/
.gradle/
*.log
```