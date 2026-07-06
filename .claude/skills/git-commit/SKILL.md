---
name: git-commit
description: Create Git commits by splitting changes into logical units following MUDDA's actual convention. Handles branch creation per issue — detects develop branch and checks out (or reuses) an issue branch before committing.
allowed-tools: Bash
---

## Step 0 — Branch Check (Required)

Check the current branch first:

```bash
git branch --show-current
```

**If current branch is `develop`:**

MUDDA uses one branch per issue. Every commit must be tied to a GitHub issue number.

1. Analyze all changes with `git status` and `git diff`
2. Determine the issue number:
   - If the user gave one, use it
   - Otherwise ask, or look it up with `gh issue list`
3. Infer branch name: `<type>/<issue-number>-<kebab-case-description>`
   - Examples (from actual history): `chore/5-initial-database-migration`, `ci/1-gradle-test-workflow`
4. Create and checkout the branch:
   ```bash
   git checkout -b <type>/<issue-number>-<kebab-description>
   ```
5. Proceed with the commit flow below

**If current branch is already an issue branch (`<type>/<issue-number>-...`)**: reuse it — do not create a new branch even if the new commit's type differs (e.g., a `fix` commit on the same issue lands on the same branch that started with `chore`). Extract the issue number from the current branch name for Step "Determine Issue Number" below.

**If current branch is neither `develop` nor an issue-branch pattern**: proceed directly to the commit flow, no issue number required.

---

## Commit Message Rules

Format: `type: #<issue-number> :: description`

- **Types**: `feat` / `fix` / `chore` / `ci` / `docs` / `refactor` / `test` (English)
- **Issue number**: required whenever the work is tied to a GitHub issue (extract from the current branch name if it follows the `<type>/<issue-number>-...` pattern; otherwise ask or look up with `gh issue list`). Omit `#<issue-number> ::` entirely only for trivial changes with no tracking issue.
- **Description**: English, lowercase start, imperative mood, no period
  - Real examples: `fix: #5 :: avoid privileged postgis migration`, `chore: #5 :: add initial database migration`, `fix: #3 :: address runtime setup review`
- Subject line only (no body)
- Do NOT add AI tool as co-author

## Determine Issue Number

```bash
git branch --show-current | grep -oE '^[a-z]+/[0-9]+' | grep -oE '[0-9]+$'
```

If this returns nothing and the change is tied to an issue, ask the user for the issue number or run `gh issue list` to find a candidate.

## Commit Flow

1. Inspect changes: `git status`, `git diff`
2. Categorize into logical units (feature / bug fix / refactoring / etc.)
3. Group files per unit
4. For each group:
   - Stage only relevant files with `git add`
   - Write a commit message following the rules above
   - `git commit -m "message"`
5. Verify with `git log --oneline -n <count>`