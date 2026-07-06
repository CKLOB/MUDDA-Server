# Agents

Claude가 특정 트리거 문구 또는 상황을 감지하면 자동으로 위임하는 서브에이전트입니다.
직접 호출할 수도 있고, Claude가 컨텍스트를 보고 자동으로 실행하기도 합니다.

`CLAUDE.md`가 아직 없어도 동작하도록 설계되어 있으며(존재 확인 후 없으면 해당 부분만 스킵), 추후 `CLAUDE.md`가 추가되면 별도 수정 없이 자동으로 그 내용을 우선 적용합니다. `.claude/`와 `.agents/`/`.codex/`(Codex 쪽)는 독립 시스템으로 취급되며 서로 동기화하지 않습니다.

## 코드 품질

| 에이전트 | 트리거 예시 | 설명 |
|---|---|---|
| [kotlin-convention-validator](./kotlin-convention-validator.md) | `컨벤션 검사해줘` | **Kotlin 전용.** git diff HEAD의 변경된 `.kt` 파일에서 컨벤션 위반 탐지 및 자동 수정 (생성자 주입, val/var, `@Transactional`, 로깅). CLAUDE.md 존재 시 그 규칙 우선, 없으면 임시 기본값 사용 |
| [contradiction-finder](./contradiction-finder.md) | `모순 찾아줘`, `일관성 검사해줘` | CLAUDE.md↔코드, 에이전트↔에이전트, CLAUDE.md↔스킬 3계층 일관성 감사. CLAUDE.md가 없으면 해당 레이어만 스킵 표시하고 나머지는 정상 수행 |

## 문서

| 에이전트 | 트리거 예시 | 설명 |
|---|---|---|
| [doc-polisher](./doc-polisher.md) | `문서 갱신해줘`, `CLAUDE.md 업데이트해줘` | 실제 코드 패턴 기반으로 CLAUDE.md/에이전트/스킬 문서 갱신. **CLAUDE.md가 없으면 no-op으로 종료**하고 그 사실만 보고 |
| [prompt-polisher](./prompt-polisher.md) | `프롬프트 다듬어줘`, `에이전트 설명 다듬어줘` | 에이전트·스킬 파일의 영문 문법·frontmatter 완성도·트리거 문구 구체성 검사. Before/After diff 형식으로 제안만 (편집 없음) |

## 개발

| 에이전트 | 트리거 예시 | 설명 |
|---|---|---|
| [kotlin-test-fixer](./kotlin-test-fixer.md) | `테스트 고쳐줘`, `<클래스명> 테스트 고쳐줘` | **Kotlin 전용.** JUnit5+MockK+Testcontainers 테스트 실행 → 실패 진단 → 수정. 서비스 코드가 진실의 원천 — 서비스 버그는 서비스에서, 동작 변경은 테스트 업데이트. 최대 3회 재시도 |

## 리서치

| 에이전트 | 트리거 예시 | 설명 |
|---|---|---|
| [web-researcher](./web-researcher.md) | `최신 정보 조사해줘`, 릴리스 노트·CVE 질의 | 실시간 웹 정보 수집. 모델 학습 데이터 이후의 최신 사실·보안 권고·라이브러리 비교가 필요할 때 |