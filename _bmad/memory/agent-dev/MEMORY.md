# Memory — Forge Patterns

_Implementation lessons. Grows over time. Keep under 200 lines._

## Known Gotchas

- **Kotlin property annotations:** Decision docs use `@get:InputFiles` / `@get:OutputDirectory` etc. When writing tests that check for annotation presence in code blocks, match the annotation name without the `@` prefix — `"InputFiles"` not `"@InputFiles"` — or the check fails.
- **Git commit before testing:** TC-1 for spike issues checks `git log -- path` for committed files. Always commit the artifact before running tests that verify it's in git history.

## Spike Issue Pattern
- Spike deliverable = decision document (already written by Atlas before Forge arrives)
- Tests = document inspection (Kotest + file reads + subprocess calls to git/gh)
- "Contracts" = helper class wrapping document access (`SpikeDecisionDocument`)
- GitHub API tests (checking issue bodies) must be guarded by `GH_TOKEN` env var

## PR Patterns
- Branch: `feature/issue-{n}-{slug}`
- Commit artifacts + build infra + tests in one commit; annotation fixes in a follow-up
- Update linked issues as part of the PR (e.g., SHADER-1 from SPIKE-SHADER-1)
