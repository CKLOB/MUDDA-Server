# Commit & Branch Conventions (MUDDA)

## Commit Message Format

`type: #<issue-number> :: description`

- **Types**: `feat` / `fix` / `chore` / `ci` / `docs` / `refactor` / `test` (English)
- **Issue number**: the GitHub issue this commit belongs to. Extract from the current branch name (`<type>/<issue-number>-<kebab>`) when possible.
- **Description**: English, lowercase start, imperative mood, no period

Real examples from this repository's history:

```
fix: #5 :: avoid privileged postgis migration
chore: #5 :: add initial database migration
fix: #3 :: address runtime setup review
chore: #3 :: add local development runtime setup
```

Commits made before an issue existed (early setup) omitted the issue number:

```
fix: update GitHub templates
chore: add GitHub workflow and templates
```

Treat this as legacy — new commits should include the issue number whenever an issue exists.

## Branch Naming — One Branch Per Issue

`<type>/<issue-number>-<kebab-case-description>`

```
chore/5-initial-database-migration
ci/1-gradle-test-workflow
chore/7-codex-development-settings
```

All commits related to the same issue land on the same branch, even across different commit types (e.g. a `chore` commit that starts the work, followed by a `fix` commit addressing review feedback on the same branch).