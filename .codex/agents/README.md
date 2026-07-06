# Agents (Codex)

OpenAI Codex가 명시적 위임 요청(예: "이 작업 두 에이전트로 나눠줘") 또는 트리거 문구를 감지하면
스폰하는 커스텀 서브에이전트입니다. 저장소에 커밋되어 팀과 공유됩니다.

각 `.toml` 파일이 에이전트 정의이며, 필수 필드는 `name` / `description` / `developer_instructions`,
선택 필드로 `model_reasoning_effort`(`low`/`medium`/`high`)와 `sandbox_mode`(`read-only`/`workspace-write`)를 사용합니다.
`AGENTS.md`가 아직 없어도 동작하도록 설계되어 있으며(존재 확인 후 없으면 해당 부분만 스킵), 추후 `AGENTS.md`가 추가되면 별도 수정 없이 자동으로 그 내용을 우선 적용합니다.

## 코드 품질

| 에이전트 | 트리거 예시 | sandbox | 설명 |
|---|---|---|---|
| [kotlin-convention-validator](./kotlin-convention-validator.toml) | `컨벤션 검사해줘` | workspace-write | **Kotlin 전용.** git diff HEAD의 변경된 `.kt` 파일에서 컨벤션 위반 탐지 및 자동 수정 (생성자 주입, val/var, `@Transactional`, 로깅). AGENTS.md 존재 시 그 규칙 우선, 없으면 임시 기본값 사용 |
| [contradiction-finder](./contradiction-finder.toml) | `모순 찾아줘`, `일관성 검사해줘` | read-only | AGENTS.md↔코드, 에이전트↔에이전트, AGENTS.md↔스킬 3계층 일관성 감사. AGENTS.md가 없으면 해당 레이어만 스킵 표시하고 나머지는 정상 수행 |

## 문서

| 에이전트 | 트리거 예시 | sandbox | 설명 |
|---|---|---|---|
| [doc-polisher](./doc-polisher.toml) | `문서 갱신해줘`, `AGENTS.md 업데이트해줘` | workspace-write | 실제 코드 패턴 기반으로 AGENTS.md/에이전트/스킬 문서 갱신. **AGENTS.md가 없으면 no-op으로 종료**하고 그 사실만 보고 |
| [prompt-polisher](./prompt-polisher.toml) | `프롬프트 다듬어줘`, `에이전트 설명 다듬어줘` | read-only | 에이전트·스킬 파일의 영문 문법·frontmatter 완성도·트리거 문구 구체성 검사. Before/After diff 형식으로 제안만 (편집 없음) |

## 개발

| 에이전트 | 트리거 예시 | sandbox | 설명 |
|---|---|---|---|
| [kotlin-test-fixer](./kotlin-test-fixer.toml) | `테스트 고쳐줘`, `<클래스명> 테스트 고쳐줘` | workspace-write | **Kotlin 전용.** JUnit5+MockK+Testcontainers 테스트 실행 → 실패 진단 → 수정. 서비스 코드가 진실의 원천 — 서비스 버그는 서비스에서, 동작 변경은 테스트 업데이트. 최대 3회 재시도 |

## 리서치

| 에이전트 | 트리거 예시 | sandbox | 설명 |
|---|---|---|---|
| [web-researcher](./web-researcher.toml) | `최신 정보 조사해줘`, 릴리스 노트·CVE 질의 | read-only | 실시간 웹 정보 수집. 모델 학습 데이터 이후의 최신 사실·보안 권고·라이브러리 비교가 필요할 때. Codex CLI 환경에서 웹 검색 기능이 활성화되어 있어야 동작 |