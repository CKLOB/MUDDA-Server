# Labels (MUDDA)

Snapshot of `gh label list` at the time this file was written. Labels can change — re-run `gh label list` before relying on this.

| Label | Description |
|---|---|
| `♻️ Refactor` | 코드 리팩토링 |
| `⚙ Setting` | 환경 세팅 |
| `⚡️ Simple` | 간단한 변경사항 |
| `⚡️performance` | 성능 개선 |
| `0️⃣ Priority: Critical` | 우선순위 - 긴급 |
| `1️⃣ Priority: High` | 우선순위 - 상 |
| `2️⃣ Priority: Medium` | 우선순위 - 중 |
| `3️⃣ Priority: Low` | 우선순위 - 하 |
| `✅ Test` | Test 관련사항 |
| `✨ Feature` | 신규 기능 |
| `🌏 Deploy` | 배포 관련 |
| `🐞 Bug` | 버그 발생 |
| `📝 Docs` | 문서가 추가/변경되는 경우 |
| `🙋‍♂️ Question` | 질문 |
| `🪡 Want` | 요청사항 |

## Commit Type → Label Suggestion

| Commit type | Suggested label |
|---|---|
| `feat` | `✨ Feature` |
| `fix` | `🐞 Bug` |
| `chore` | `⚙ Setting` |
| `ci` | `🌏 Deploy` |
| `docs` | `📝 Docs` |
| `test` | `✅ Test` |
| `refactor` | `♻️ Refactor` |

Priority labels (`0️⃣`–`3️⃣`) are never auto-assigned — leave that to human judgment.

**Note**: `.github/ISSUE_TEMPLATE/bug.yml` and `todo.yml` default to labels `버그`/`할일`, which do not exist in the current label set above (a pre-existing mismatch in this repo, out of scope here). Do not rely on those issue-template labels when selecting PR labels.