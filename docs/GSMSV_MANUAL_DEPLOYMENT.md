# GSMSV 수동 운영 배포

이 문서는 MUDDA를 GSMSV 학교 VM에 최초 수동 배포하기 위한 절차입니다. 이번 구성은 GitHub Actions CD, GHCR, Discord 알림, Nginx, Certbot 없이 Docker Compose로 실행합니다.

## 배포 구조

```text
https://mudda-api.https.gsmsv.site
        │ GSMSV HTTPS Reverse Proxy
        ▼
VM:8080 ── app:8080 ── backend network ── postgres:5432
                                      └── redis:6379
                     app ── AWS S3 / Firebase
```

GSMSV 설정값은 다음과 같습니다.

- 서브도메인: `mudda-api`
- 내부 전달 포트: `8080`
- 예상 주소: `https://mudda-api.https.gsmsv.site`
- VM에서는 Nginx와 Certbot을 설치하거나 설정하지 않습니다.

PostgreSQL과 Redis는 Compose 내부 `backend` 네트워크에만 연결되며 호스트 포트를 publish하지 않습니다. 외부에 publish되는 포트는 App의 `8080`뿐입니다.

현재 애플리케이션은 Redis 비밀번호 설정을 사용하지 않습니다. Redis는 외부 포트가 없고 `internal` 네트워크에만 연결되므로 이번 범위에서는 네트워크 격리로 보호합니다. Redis 인증을 추가하려면 Spring 설정, Redis command, healthcheck, Secret 전달 방식을 함께 변경해야 하므로 별도 후속 작업으로 남깁니다.

## 요구 사양

- Ubuntu 22.04.5 LTS, x86_64, 2 vCPU, RAM 7.9GB, 디스크 40GB
- Docker Engine과 Docker Compose v2
- GSMSV Project Owner VM 및 HTTPS Reverse Proxy 설정 권한
- AWS S3 bucket과 최소 권한 AWS credential
- OAuth provider credential
- 선택적으로 Firebase service-account JSON

Docker는 Ubuntu 공식 설치 절차에 따라 설치한 뒤 다음을 확인합니다.

```bash
docker --version
docker compose version
```

## 운영 환경 준비

저장소를 VM의 배포 디렉터리에 clone한 뒤 실제 운영 환경 파일을 만듭니다.

```bash
cp .env.production.example .env.production
chmod 600 .env.production
mkdir -p /opt/mudda/secrets
chmod 700 /opt/mudda/secrets
```

`.env.production`의 모든 `REPLACE_WITH_*` 값을 실제 값으로 교체합니다. 이 파일과 Firebase credential은 절대 Git에 커밋하지 않습니다. `.gitignore`는 `.env.*`와 `secrets/`를 제외하며 예시 파일만 예외로 둡니다.

`MUDDA_MASTER_KEY`는 기존 `EncryptedStringConverter` 데이터의 복호화에 사용됩니다. 최초 설정 시 별도 보관 위치에 백업하고, 분실하거나 임의로 변경하면 기존 봉투 암호화 캡슐을 복구할 수 없습니다. 키를 문서·Issue·로그·Docker image에 기록하지 않습니다.

예시 값은 생성하지 말고 VM에서 직접 생성하여 운영 환경 파일과 별도 백업에만 기록합니다.

```bash
openssl rand -base64 32  # MUDDA_MASTER_KEY
openssl rand -base64 48  # JWT_SECRET
openssl rand -hex 24     # DB_PASSWORD
```

FCM을 사용할 경우 service-account JSON을 다음 경로에 배치합니다.

```text
/opt/mudda/secrets/firebase-service-account.json
```

그리고 `FCM_ENABLED=true`를 설정합니다. 사용하지 않을 때는 `FCM_ENABLED=false`로 두면 credential 없이도 앱이 시작됩니다.

## 이미지 빌드 및 실행

운영 환경 파일을 명시하여 Compose 변수 치환과 App 환경변수가 같은 파일을 사용하도록 합니다.

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml build --pull
docker compose --env-file .env.production -f docker-compose.prod.yml up -d
```

Dockerfile은 Java 21 JDK 빌드 스테이지와 Java 21 JRE 실행 스테이지를 사용합니다. Gradle Wrapper로 `bootJar`를 빌드하고, 실행 컨테이너는 비 root `appuser`로 동작합니다. Secret과 Firebase JSON은 이미지에 복사되지 않습니다.

운영 Profile은 `SPRING_PROFILES_ACTIVE=prod`이며 Compose가 이를 보장합니다. `spring.docker.compose.enabled=false`로 앱이 로컬 개발용 `docker-compose.yml`을 다시 자동 실행하지 않도록 합니다. Flyway가 전체 마이그레이션을 적용하고 Hibernate는 `ddl-auto=validate`로 스키마를 변경하지 않습니다.

## 최초 실행 및 상태 확인

컨테이너 상태와 healthcheck를 확인합니다.

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml ps
docker compose --env-file .env.production -f docker-compose.prod.yml logs --tail=200 app
curl --fail https://mudda-api.https.gsmsv.site/actuator/health
curl --fail https://mudda-api.https.gsmsv.site/actuator/health/readiness
```

최초 App 로그에서 Flyway가 V1부터 최신 버전까지 적용되었는지 확인합니다. readiness는 `readinessState`, PostgreSQL(`db`), Redis(`redis`)를 포함하므로 DB나 Redis가 연결되지 않으면 정상 상태가 되지 않습니다. liveness는 애플리케이션 프로세스 상태만 확인합니다. Actuator 외부 노출 범위는 `health` 하나이며 세부 정보는 노출하지 않습니다.

SecurityConfig는 `/actuator/health/**`만 인증 없이 허용하고, 다른 API는 기존처럼 인증을 요구합니다.

호스트 포트와 볼륨을 확인합니다.

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml port app 8080
docker compose --env-file .env.production -f docker-compose.prod.yml config | grep -E 'ports:|8080:8080|5432|6379'
docker volume ls | grep mudda-prod
```

Compose 파일에 PostgreSQL `5432`나 Redis `6379`의 `ports` 항목은 없습니다. 위 출력에는 App의 `8080:8080`만 있어야 합니다. `docker compose config`나 로그에 실제 Secret이 표시되지 않도록 실제 운영 파일을 출력·공유하지 않습니다.

Flyway와 PostgreSQL 데이터는 `mudda-prod-postgres-data`에, Redis AOF는 `mudda-prod-redis-data`에 보존됩니다. Redis는 refresh token과 access-token blacklist의 TTL 상태를 재시작 뒤에도 유지하기 위해 AOF를 사용합니다. 볼륨은 백업을 대체하지 않으므로 PostgreSQL 백업은 별도 후속 작업입니다.

## 운영 관리

```bash
# 앱만 재시작
docker compose --env-file .env.production -f docker-compose.prod.yml restart app

# 전체 서비스의 새 이미지 반영
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build

# 정상 종료(SIGTERM 후 Spring graceful shutdown 대기)
docker compose --env-file .env.production -f docker-compose.prod.yml down
```

`restart: unless-stopped`가 적용되어 VM 재부팅이나 프로세스 장애 뒤 서비스를 다시 시작합니다. 로그는 컨테이너별 `json-file` 기준 파일당 10MB, 최대 3개로 제한됩니다. 운영 데이터가 있는 상태에서 `down -v`, `docker volume prune`, 전체 컨테이너 삭제 명령을 사용하지 않습니다.

JVM은 기본적으로 컨테이너 메모리의 65%를 최대 힙으로 사용하고, `JAVA_TOOL_OPTIONS`로 조정할 수 있습니다. App 1GB, PostgreSQL 768MB, Redis 256MB의 컨테이너 메모리 상한과 총 2 vCPU 이내의 CPU 상한을 사용합니다. 스케줄러와 JVM 시간대는 `Asia/Seoul`로 맞춥니다.

## 최초 수동 배포 검증

다음 항목을 실제 VM에서 확인해야 합니다.

- Docker image build 및 Compose 기동 성공
- `postgres`, `redis`, `app` healthcheck가 모두 `healthy`
- Flyway 전체 적용 및 PostgreSQL/PostGIS 연결
- App readiness와 외부 HTTPS 주소 응답
- Redis 연결 및 애플리케이션 기능 확인
- `8080`만 외부 공개되고 `5432`, `6379`는 공개되지 않음
- 컨테이너 재시작 뒤 DB·Redis 볼륨과 앱 연결 유지
- 실제 환경 파일·Firebase JSON·Secret이 Git, image history, 로그에 없음

이번 PR은 운영 기반 파일과 수동 배포 문서만 추가합니다. GSMSV VM 최초 배포, PostgreSQL 백업, develop 기반 GitHub Actions CD, Discord 웹훅 임베드 알림은 후속 작업입니다.
