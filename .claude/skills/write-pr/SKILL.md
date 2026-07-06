---
name: write-pr
description: Generate PR title, body, and labels from commits since develop, then create the PR on GitHub. Follows MUDDA's actual PR template and title convention.
allowed-tools: Bash(git *:*), Bash(bash *create-pr.sh:*), Bash(cat *:*), Read, Write
---

## Step 1 — Gather Context

```bash
git branch --show-current
git log origin/develop..HEAD --oneline 2>/dev/null || git log --oneline -15
git diff origin/develop...HEAD --stat 2>/dev/null || git diff HEAD~5...HEAD --stat
git diff origin/develop...HEAD 2>/dev/null || git diff HEAD~5...HEAD
```

Also read the PR template:

```bash
cat .github/PULL_REQUEST_TEMPLATE.md
```

If the template file is missing, fall back to the structure hardcoded in Step 3.

## Step 2 — Determine Issue Number and Labels

Extract the issue number from the branch name (`<type>/<issue-number>-<kebab>`, see `.claude/skills/git-commit/SKILL.md`).

Re-fetch the current label set before selecting — labels can change over time:

```bash
gh label list
```

Read `.claude/skills/write-pr/references/labels.md` for the last-known label set and a commit-type → label mapping suggestion, but treat the live `gh label list` output as authoritative. Select 0–2 labels; labeling is a convention, not a hard requirement (past PRs were often unlabeled).

## Step 3 — Generate PR Content

**Title** — Korean, no prefix of any kind (no `type:`, no `[scope]`, no brackets). A short, concise description of what the PR does, e.g. `초기 데이터베이스 마이그레이션 추가`. Generate 2–3 options and let the user pick.

**Body** — Follow the structure of `.github/PULL_REQUEST_TEMPLATE.md`:

```markdown
## ✨ 작업 내용
<이번 PR에서 어떤 작업을 했는지 요약>

---

## 🔍 리뷰 시 참고사항
<변경 이유, 배경, 고려했던 점>

---

## ✅ 체크리스트
- [ ] 문서(README, `.env.example` 등) 변경이 필요한 경우 작성 또는 수정했나요?
- [ ] 작업한 코드가 정상적으로 동작하는 것을 직접 확인했나요?
- [ ] 필요한 경우 테스트 코드를 작성하거나 수정했나요?
- [ ] Merge 대상 브랜치를 올바르게 설정했나요?
- [ ] PR에 관련 없는 작업이 포함되지 않았나요?
- [ ] 적절한 라벨과 리뷰어를 설정했나요?

---

## 📎 관련 이슈(선택)
- Close #<issue-number>
```

Rules:
- Korean 합쇼체: `~하였습니다`, `~되었습니다`, `~추가하였습니다`
- No emojis beyond the template's own section headers
- Wrap class names, method names, annotations, file names in backticks

## Step 4 — Write Body & Show Preview

Write the body to `PR_BODY.md`, then display:

```
## PR 제목 후보
1. [title1]
2. [title2]

## 선택된 라벨
- label1 (or none)

## PR 본문 미리보기
[body content]
```

Ask the user which title to use. Wait for the answer before proceeding.

## Step 5 — Create PR

```bash
bash .claude/skills/write-pr/scripts/create-pr.sh "<confirmed-title>" "PR_BODY.md" "<label1>,<label2>"
```

The script targets `develop` as the base branch by default (MUDDA's actual PRs all merge into `develop`).

After creation, display the PR URL. Cleanup: remove `PR_BODY.md`.