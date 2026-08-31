# GSMSV CD 운영 가이드

이 문서는 `develop`에 반영된 MUDDA 이미지를 GHCR에 저장하고 GSMSV VM으로 배포하는 수동 확인 가능한 CD Workflow의 운영 절차를 정의한다. 실제 Secret 등록과 서버 배포는 이 변경에 포함하지 않는다.

## 동작 범위

`.github/workflows/deploy-develop.yml`은 `develop` push와 수동 `workflow_dispatch`를 대상으로 한다. 자동 배포는 Repository Variable `CD_ENABLED`가 정확히 `true`일 때만 실행된다. 값이 없거나 `false`이면 push 배포 Job은 skip된다. 수동 실행은 기본 `dry_run=true`이며, 이 모드에서는 Docker 이미지 빌드와 검증만 수행하고 SSH, 서버 변경, GHCR push, Discord 전송을 수행하지 않는다.

실제 배포를 활성화하기 전 다음 Secret을 등록해야 한다.

- `DEPLOY_HOST`
- `DEPLOY_PORT`
- `DEPLOY_USER`
- `DEPLOY_SSH_KEY` (비밀번호가 아닌 전용 SSH private key)
- `DISCORD_DEPLOY_WEBHOOK_URL`

선택 Variable은 `DEPLOY_PATH`이며 기본값은 `/opt/mudda`이다. `CD_ENABLED=true` 설정 전에는 dry-run으로 Workflow와 이미지 빌드만 확인한다. Secret 값은 이 저장소, Workflow 로그, Issue, PR, Discord에 기록하지 않는다.

## 이미지와 서버 구조

기존 `Dockerfile`을 Buildx로 빌드하며 commit의 40자리 SHA를 불변 이미지 태그로 사용한다.

```text
ghcr.io/cklob/mudda-server:<commit-sha>
ghcr.io/cklob/mudda-server:develop
```

배포 서버에는 `/opt/mudda/docker-compose.prod.yml`, `/opt/mudda/.env.production`, PostGIS 초기화 SQL 파일과 Firebase credential 디렉터리가 이미 준비되어 있어야 한다. 서버의 `.env.production`은 Actions로 가져오거나 출력하지 않는다. GHCR private package를 사용할 경우 서버 Docker daemon에 최소 read 권한의 사전 인증을 구성해야 하며, 이 작업에서 PAT나 Secret을 생성하지 않는다.

`docker-compose.prod.yml`은 `APP_IMAGE`가 있으면 해당 이미지를 사용하고, 없으면 기존 `mudda-app:prod` fallback을 사용한다. 따라서 기존 수동 `docker compose -f docker-compose.prod.yml build` 및 `up -d` 방식은 유지된다. 배포 스크립트는 PostgreSQL·Redis 볼륨을 삭제하거나 재생성하지 않으며 `down -v`, prune 명령을 실행하지 않는다.

## 배포 판정과 rollback

새 이미지를 pull한 뒤 app만 갱신하고 Compose healthcheck를 최대 30회, 5초 간격으로 확인한다. readiness가 healthy가 되어야 성공으로 판정한다. 실패하면 이전에 실행 중이던 SHA 이미지가 확인되는 경우에만 app을 이전 이미지로 되돌리고 readiness를 다시 확인한다. rollback은 app 컨테이너에만 적용되며 DB migration을 되돌리지 않는다.

Flyway migration이 이미 적용된 뒤 이전 애플리케이션으로 rollback하면 스키마와 애플리케이션 간 비호환이 생길 수 있다. 따라서 breaking migration은 사전 호환성을 검토하고, 자동 DB rollback은 수행하지 않는다. 실패 시 Actions 로그에는 Compose 상태와 app의 최근 일부 정제 로그만 남긴다.

## Discord 알림

배포 시작·성공·실패를 `DISCORD_DEPLOY_WEBHOOK_URL` Secret으로만 전송한다. 실패 알림에는 실패 단계, 종료 코드, rollback 결과, 실행 URL과 최대 약 2,500자의 정제 로그만 포함한다. webhook URL, JWT, Authorization, Cookie, password, AWS key, 이메일 등 민감정보 패턴을 마스킹하며 전체 로그나 `.env.production`은 전송하지 않는다. Webhook 전송 실패는 원래 배포 결과를 성공으로 바꾸지 않는다.

## 최초 활성화 절차

1. 서버에 Docker Compose 파일, 운영 `.env.production`, credential 파일을 준비하고 권한을 확인한다.
2. GitHub Actions에 전용 SSH key와 위 Secret을 등록한다. private GHCR package라면 서버 pull 인증도 별도로 준비한다.
3. `workflow_dispatch`에서 기본 `dry_run=true`를 실행해 Workflow와 이미지 빌드를 확인한다.
4. 테스트 결과를 확인한 뒤 Repository Variable `CD_ENABLED=true`를 설정한다.
5. 다음 `develop` 반영에서 실제 배포와 readiness, Discord 알림을 확인한다.

실제 Secret 등록, Workflow 실행, SSH 접속, 서버 배포는 이 작업에서 수행하지 않았다. 장애 시 서버에서 `docker compose -f docker-compose.prod.yml ps`, `docker compose -f docker-compose.prod.yml logs --tail 100 app`으로 확인하고, 필요하면 문서화된 SHA 이미지로 수동 rollback하되 DB migration 위험을 먼저 검토한다. Nginx와 Certbot은 사용하지 않는다.
