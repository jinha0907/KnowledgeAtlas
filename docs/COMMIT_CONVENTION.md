# Commit Message Convention

Use Conventional Commits for every change:

```
<type>(<scope>): <short summary>
```

## Allowed types
- `feat`: new feature
- `fix`: bug fix
- `refactor`: internal code change without behavior change
- `test`: test additions/changes
- `docs`: documentation only
- `chore`: tooling/build/config/maintenance

## Scope rules
- Prefer app/domain scopes: `api`, `web`, `infra`, `docs`, `notion`, `retrieval`, `decision`.
- Use one scope unless cross-cutting is unavoidable.

## Message rules
- Use imperative present tense (e.g., `add`, `update`, `remove`).
- Keep summary under ~72 chars.
- Describe user-visible intent, not implementation detail.

## Examples
- `feat(api): add notion sync endpoint`
- `fix(api): include block text in checksum detection`
- `feat(retrieval): add deterministic search citations`
- `test(api): add decision status transition tests`
- `docs: update data model for phase3 indexes`

## Commit splitting policy
- One logical change per commit.
- Separate schema migration, API behavior, tests, and docs when possible.
- If a migration changes behavior, include minimal required code in same commit.

## Pre-push checklist
1. `mvn test` passes for `apps/api`.
2. Related docs are updated (`docs/*`, `plans/*` when needed).
3. No secrets are staged (`.env` excluded, `.env.example` only placeholders).
