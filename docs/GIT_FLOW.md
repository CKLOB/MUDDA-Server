# Git Flow

## 브랜치 역할

- `main`: 운영·배포 안정 브랜치이자 GitHub 기본 브랜치
- `develop`: 기능 통합 브랜치
- `feature/*`, `fix/*`, `chore/*`: 작업 브랜치

기능·수정·설정 변경 PR은 기본적으로 `develop`을 대상으로 생성합니다. `develop → main`은 별도의 릴리스 과정으로 진행하며, 기능 PR을 `main`에 직접 병합하지 않습니다.

## Issue 연결

완료한 Issue는 `develop` 대상 PR 본문에서 공식 closing keyword로 명시합니다.

지원하는 keyword는 `close`, `closes`, `closed`, `fix`, `fixes`, `fixed`, `resolve`, `resolves`, `resolved`이며 대소문자를 구분하지 않습니다. `Closes #32`, `CLOSES: #32`, `Resolves: #34`처럼 작성할 수 있습니다.

```text
Closes #32
Fixes #33
Resolves: #34
```

여러 Issue를 종료하려면 각 번호에 keyword를 반복해서 작성합니다. `Closes #32, #33`처럼 두 번째 번호에 keyword가 없는 축약형은 지원하지 않습니다. `Related #N`, `Refs #N`, 단순 `#N`, PR 제목·브랜치·커밋 메시지의 번호는 자동 종료 대상으로 사용하지 않습니다.

## 자동 종료 Workflow

`.github/workflows/close-develop-issues.yml`은 PR이 실제로 병합되어 닫힌 경우에만 실행되며, base가 정확히 `develop`인 같은 저장소 PR 본문을 검사합니다. 연결된 같은 저장소의 열린 Issue만 검증한 뒤 감사 Comment를 남기고 `completed` 사유로 종료합니다. `main` 대상 릴리스 PR이나 닫히기만 한 미병합 PR은 처리하지 않습니다.

Workflow는 PR Head 코드를 실행하지 않고 기본 브랜치 `main`의 자동화 코드를 사용합니다. 따라서 이 자동화 PR이 `main`에 병합된 이후부터 동작하며, 병합 후 `main → develop` 동기화가 필요합니다.

보안상 외부 Fork에서 생성된 PR은 자동 종료 대상에서 제외합니다. Fork PR의 `GITHUB_TOKEN`은 Issue 쓰기 권한이 제한될 수 있으므로, 권한을 확대하거나 `pull_request_target`을 사용하지 않고 같은 저장소 브랜치의 PR만 처리합니다.
