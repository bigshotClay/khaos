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
- `.environment().also { it.clear(); it["PATH"] = System.getenv("PATH") ?: "/usr/bin:/bin" }` — scoped env; no CI secret leakage; always restore PATH after clear or git binary won't be found
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

## Gradle 9.4.1 buildSrc Gotchas (Issue #3)
- **TOML `classifier` key**: NOT supported in `[libraries]` entries. LWJGL native variants cannot go in the catalog — declare inline with `variantOf()` or `"g:a::classifier"` notation.
- **`libs` type-safe accessor in buildSrc plugins**: NOT generated for precompiled script plugins. Use `the<VersionCatalogsExtension>().named("libs").findLibrary("key").orElseThrow { GradleException("...") }` at runtime instead. Always guard with orElseThrow, not bare .get().
- **Kotlin plugin + buildSrc classpath conflict**: Adding `kotlin-gradle-plugin` to buildSrc `implementation` puts it on the main build's script classpath. Re-declaring at root with `alias(libs.plugins.kotlin.multiplatform) apply false` fails ("already on classpath with unknown version"). Fix: omit Kotlin plugin declarations at root; only declare plugins not already on buildSrc classpath (e.g., KSP).
- **Convention plugin file names must match plugin ID**: `khaos.kotlin-kmp.gradle.kts` → ID `khaos.kotlin-kmp`. Test plans may have naming bugs if the ID prefix isn't in the filename.
- **env.clear() must restore PATH**: After `ProcessBuilder.environment().clear()`, always set `["PATH"] = System.getenv("PATH") ?: "/usr/bin:/bin"`. Without PATH, git/other binaries are not found on non-standard runners.
