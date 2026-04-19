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

## Subprocess Safety (ProcessBuilder)
When a test uses `ProcessBuilder` to call `gh` or `git`:
- `redirectErrorStream(true)` — always; prevents deadlock if stderr fills OS pipe buffer
- `waitFor(30, TimeUnit.SECONDS)` — always; prevents CI hang
- `.environment().also { it.clear(); it["GH_TOKEN"] = token }` — scoped env; no CI secret leakage
All three are required. Forgetting any one will cause a Gauntlet finding.

## Issue Body vs. Comment
`gh issue comment N` does NOT satisfy "issue updated" ACs. The AC requires editing the body:
`gh issue edit N --body "..."`. This is a recurring source of shadow findings.

## Spike Skeleton Error Handling
Any stub method (`parseXxx`, `validateXxx`) in a spike skeleton must have a comment stating
either fail-fast behavior ("Fail-fast: validate required fields; throw with shader name on missing field")
or explicit deferral ("error handling deferred to implementation phase"). Silent stubs = TC-16-class shadow.

## lazy(LazyThreadSafetyMode.NONE) in Document Helpers
For `SpikeDecisionDocument.text` or any per-test lazy doc accessor, use `LazyThreadSafetyMode.NONE`
so a missing-document exception retries cleanly on each access. Without this, cascade failures obscure
which tests would otherwise pass.

## PR Patterns
- Branch: `feature/issue-{n}-{slug}`
- Commit artifacts + build infra + tests in one commit; annotation fixes in a follow-up
- Update linked issues as part of the PR (e.g., SHADER-1 from SPIKE-SHADER-1)
