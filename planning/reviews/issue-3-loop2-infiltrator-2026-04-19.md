# Gauntlet Loop 2 — The Infiltrator (Security) Re-Review
## Issue #3: F-1 Gradle Multi-Module Scaffolding — PR #37

**Date:** 2026-04-19
**Reviewer:** The Infiltrator (Security)
**Loop:** 2 — verifying loop 1 fixes and hunting regressions

---

## Loop 1 Fix Verification

### INF-01: RESOLVED (with regression — see INF-L2-01 below)
`pb.environment().clear()` is now present at `SpikeDecisionDocument.kt:24`. The inherited-environment leakage is closed. However, the fix is incomplete: `PATH` and `HOME` are not restored after clearing. A new HIGH finding is raised for this regression.

### INF-02: RESOLVED (shader specs only; pattern does not recur in committedToGit)
In `SpikeShader1DecisionSpec.kt` and `SpikeShader2DecisionSpec.kt`, the fix replaced the bare fall-through with `withClue("gh subprocess must exit within 30 seconds") { exited shouldBe true }` at line 120 / line 111 respectively. When `exited == false` the assertion fires before `bodyFuture.get(5, TimeUnit.SECONDS)` is reached, so `CancellationException` no longer surfaces. The `committedToGit()` implementation in `SpikeDecisionDocument.kt` correctly uses `return false` inside the timeout branch (line 31), avoiding the same defect independently.

### INF-03: RESOLVED
`path.parent ?: return false` at `SpikeDecisionDocument.kt:19` guards the null case. `!parent.toFile().exists()` at line 20 guards the missing-directory case. TC-29 in `SpikeDecisionDocumentEdgeCaseSpec.kt` covers both paths. Fix is correct and complete.

### INF-05: RESOLVED — SHA-256 correct
`distributionSha256Sum=2ab2958f2a1e51120c326cad6f385153bb11ee93b3c216c5fccebfdfbb7ec6cb` added at `gradle/wrapper/gradle-wrapper.properties:4`. The value is 64 hex characters (verified) and matches the official Gradle 9.4.1-bin.zip checksum published on gradle.org/release-checksums/. `validateDistributionUrl=true` was already present. Wrapper integrity is now pinned.

### INF-06: NOT FIXED
`ksp = "2.3.6"` remains unchanged in `gradle/libs.versions.toml:3`. The phantom version is still present. No fix applied.

### INF-07: NOT FIXED
`private val projectRoot = Paths.get(System.getProperty("user.dir"))` at `SpikeShader1DecisionSpec.kt:12` and `SpikeShader2DecisionSpec.kt:11`. Both files still trust `user.dir` without sandboxing. No fix applied.

### INF-08: NOT FIXED
`val text: String by lazy(LazyThreadSafetyMode.NONE)` at `SpikeDecisionDocument.kt:13`. Unchanged. No fix applied.

### INF-09: NOT FIXED
`System.getenv("GH_TOKEN") ?: System.getenv("GITHUB_TOKEN")` — precedence still undocumented. No fix applied.

### INF-10 (COR-04): RESOLVED
Both convention plugins now use `.orElseThrow { GradleException("Catalog entry '...' not found in libs catalog") }`:
- `khaos.kotlin-kmp.gradle.kts:19-20`
- `khaos.kotlin-jvm.gradle.kts:16-17`

The cryptic `NoSuchElementException` is replaced with an actionable Gradle build failure. Fix is correct in both files.

### INF-11: NOT FIXED
`freeCompilerArgs.add("-Xcontext-parameters")` remains in both `khaos.kotlin-kmp.gradle.kts:12` and `khaos.kotlin-jvm.gradle.kts:11`. Acknowledged as LOW; no fix applied.

### COR-05 (jacocoTestReport): RESOLVED
`khaos.kotlin-jvm.gradle.kts` now uses `dependsOn(tasks.test)` (line 25) instead of `finalizedBy`. `jacocoTestReport` will no longer run after a failed test task. Fix is correct.

---

## New Findings — Loop 2

---

### [HIGH] INF-L2-01: env.clear() strips PATH — git subprocess cannot be found on any non-macOS-default system
**Reviewer:** The Infiltrator (Security)
**Location:** `khaos-test-harness/src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:24`
**Category:** regression / availability

The fix for INF-01 added `pb.environment().clear()` but did not restore any environment variables afterward. `ProcessBuilder` resolves executable names using the child process's `PATH`. With an empty environment, `PATH` is absent, so the OS falls back to an implementation-defined default search — on Linux this is typically empty or `/usr/bin:/bin`; on macOS it is unspecified. `git` is commonly installed at `/usr/bin/git` on macOS stock systems, which may make this work by coincidence on developer machines. On typical CI runners (Ubuntu, Debian, Alpine Docker) `git` lives at `/usr/bin/git` or `/usr/local/bin/git` depending on the installation method; with an empty `PATH`, `ProcessBuilder` will throw `IOException: error=2, No such file or directory` at `pb.start()`.

Contrast with the correct pattern used in both shader specs (e.g., `SpikeShader1DecisionSpec.kt:107-112`):

```kotlin
pb.environment().also {
    it.clear()
    it["PATH"] = System.getenv("PATH") ?: "/usr/bin:/bin"
    it["HOME"] = System.getenv("HOME") ?: ""
    it["GH_TOKEN"] = token
}
```

`SpikeDecisionDocument.committedToGit()` performs the clear but then starts the process immediately with no PATH restoration. This is a security-correct fix that is operationally broken — it will cause `committedToGit()` to throw `IOException` on any CI runner where `git` is not in the OS-default executable search path.

**Evidence:**
```kotlin
// SpikeDecisionDocument.kt:24-25
pb.environment().clear()          // PATH is now absent
val result = pb.start()           // ProcessBuilder resolves "git" with no PATH → IOException on CI
```

**Impact:** Every test that calls `doc.committedToGit()` will throw an uncaught `IOException` on CI runners where `/usr/bin/git` is not the git binary location. TC-1 in both shader specs will fail with a confusing JVM exception rather than a git-integration failure. The regression was masked on macOS developer machines because `/usr/bin/git` is present there. It will detonate first on Linux CI.

**Fix:** After `pb.environment().clear()`, restore `PATH` and `HOME` exactly as the shader specs do:
```kotlin
pb.environment().clear()
pb.environment()["PATH"] = System.getenv("PATH") ?: "/usr/bin:/bin"
pb.environment()["HOME"]  = System.getenv("HOME")  ?: ""
```
No token is needed for `git log`.

---

### [MEDIUM] INF-L2-02: TC-29 null-parent test covers Path.of("bare-file.md") but not Path.of("/")
**Reviewer:** The Infiltrator (Security)
**Location:** `khaos-test-harness/src/test/kotlin/khaos/spike/SpikeDecisionDocumentEdgeCaseSpec.kt:11-16`
**Category:** test-gap

TC-29 verifies two paths: a bare filename `Path.of("bare-file.md")` and a non-existent parent `/nonexistent/dir/file.md`. The original INF-03 finding also cited `Path.of("/")` as a trigger case. In Kotlin/Java, `Path.of("/").parent` returns `null` — the fix at line 19 (`path.parent ?: return false`) handles this correctly in the implementation. However, TC-29 does not include a test case for `Path.of("/")`. This is not a production defect (the code is correct), but it is a test coverage gap: if a future refactor removes the null-guard, TC-29 will not catch the regression for the root-path case.

**Evidence:**
```kotlin
// TC-29 covers:
Path.of("bare-file.md")          // parent == null → return false ✓
Path.of("/nonexistent/dir/file.md") // parent exists as string, but dir absent → return false ✓
// Missing:
Path.of("/")                     // parent == null → return false (untested)
```

**Impact:** Low operational impact now (code is correct). Moderate test-debt impact: the null-guard regression case is not fully covered, creating a silent blind spot for future refactors.

---

### [MEDIUM] INF-L2-03: INF-06 (KSP phantom version) not addressed — supply-chain window remains open
**Reviewer:** The Infiltrator (Security)
**Location:** `gradle/libs.versions.toml:3`
**Category:** supply-chain / platform

`ksp = "2.3.6"` is unchanged from loop 1. The KSP versioning scheme for Kotlin 2.x is `<kotlinMajor.Minor.Patch>-<kspRelease>` (e.g., `2.3.20-1.0.25`). The version `2.3.6` does not exist in the KSP release index and cannot be resolved from Maven Central or the Gradle Plugin Portal under normal conditions.

The window identified in INF-06 remains fully open: if a malicious actor publishes `com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.3.6` to Maven Central before this is corrected, Gradle will resolve and execute that artifact as a build plugin with full build-script privileges on every machine that runs `./gradlew` with ksp applied to any module. The current `apply false` in the root build file defers — but does not eliminate — this risk.

**Evidence:**
```toml
# gradle/libs.versions.toml:3 — unchanged from loop 1
ksp = "2.3.6"    # does not exist; correct form for Kotlin 2.3.20 would be e.g. "2.3.20-1.0.25"
```

---

### [LOW] INF-L2-04: INF-07 (user.dir path traversal) not addressed — file-read injection window persists
**Reviewer:** The Infiltrator (Security)
**Location:** `SpikeShader1DecisionSpec.kt:12`, `SpikeShader2DecisionSpec.kt:11`
**Category:** injection

`System.getProperty("user.dir")` is still the project-root anchor in both spec files. The `khaos-test-harness/build.gradle.kts` sets `workingDir = rootProject.projectDir`, which aligns `user.dir` with the project root when tests are run via Gradle. However, in IntelliJ IDEA's built-in test runner, `user.dir` is typically set to the module directory, not the project root — causing the resolve chain to target a different absolute path. More critically, `JAVA_TOOL_OPTIONS=-Duser.dir=/tmp` overrides this at the JVM level, bypassing the Gradle `workingDir` setting entirely.

No fix was applied in loop 2.

---

### [LOW] INF-L2-05: INF-08 (LazyThreadSafetyMode.NONE) not addressed
**Reviewer:** The Infiltrator (Security)
**Location:** `khaos-test-harness/src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:13`
**Category:** trust / concurrency

`val text: String by lazy(LazyThreadSafetyMode.NONE)` is unchanged. The data-race risk under parallel Kotest execution documented in INF-08 persists. No Kotest project config enforcing sequential spec isolation is present. Acknowledged as LOW; no fix applied in loop 2.

---

## Summary Table

| ID | Loop 1 ID | Severity | Title | Status |
|----|-----------|----------|-------|--------|
| INF-L2-01 | *(new)* | HIGH | env.clear() strips PATH — git not found on CI | NEW FINDING |
| INF-L2-02 | *(new)* | MEDIUM | TC-29 missing Path.of("/") null-parent test case | NEW FINDING |
| INF-L2-03 | INF-06 | MEDIUM | KSP phantom version 2.3.6 — supply-chain window | NOT FIXED |
| INF-L2-04 | INF-07 | LOW | user.dir path traversal risk persists | NOT FIXED |
| INF-L2-05 | INF-08 | LOW | LazyThreadSafetyMode.NONE race not addressed | NOT FIXED |

### Loop 1 Findings — Resolution Status

| ID | Severity | Title | Resolution |
|----|----------|-------|------------|
| INF-01 | HIGH | git subprocess env inheritance | RESOLVED (fix present; regression INF-L2-01 raised) |
| INF-02 | MEDIUM | CancellationException after cancel(true) | RESOLVED (shader specs: assertion guards fall-through; committedToGit: explicit return false) |
| INF-03 | MEDIUM | path.parent null NPE | RESOLVED |
| INF-05 | HIGH | No distributionSha256Sum | RESOLVED (correct 64-char SHA-256 value added) |
| INF-06 | MEDIUM | KSP phantom version 2.3.6 | NOT FIXED → carried as INF-L2-03 |
| INF-07 | MEDIUM | user.dir trusted as project root | NOT FIXED → carried as INF-L2-04 |
| INF-08 | LOW | LazyThreadSafetyMode.NONE race | NOT FIXED → carried as INF-L2-05 |
| INF-09 | LOW | GH_TOKEN fallback undocumented | NOT FIXED (acknowledged LOW) |
| INF-10 / COR-04 | HIGH | findLibrary().get() no guard | RESOLVED (orElseThrow in both convention plugins) |
| INF-11 | LOW | -Xcontext-parameters global | NOT FIXED (acknowledged LOW) |
| COR-05 | MEDIUM | jacocoTestReport after failed test | RESOLVED (dependsOn replaces finalizedBy) |

### Verdict: REQUEST CHANGES

**Blocking (must fix before merge):**
- **INF-L2-01** [HIGH]: The env.clear() fix will cause `IOException` on Linux CI runners — `git` cannot be found without `PATH` restoration. This is an operational regression introduced by the INF-01 security fix.

**Non-blocking but recommended before merge:**
- **INF-L2-03** [MEDIUM]: KSP `2.3.6` phantom version — supply-chain risk grows with each passing day. Should be corrected to the real KSP version for Kotlin 2.3.x (e.g., `2.3.20-1.0.25` if that exists, or the nearest valid release).

**Deferred acceptable:**
- INF-L2-02, INF-L2-04, INF-L2-05 (LOW/MEDIUM test-coverage gaps and low-exploitation-likelihood issues)
