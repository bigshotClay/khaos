# Gauntlet Loop 2 — Coroner Re-Review
## Issue #3: F-1 Gradle Multi-Module Project Scaffolding — PR #37

**Date:** 2026-04-19
**Reviewer:** The Coroner (failure-path analysis)
**Loop:** 2 — verifying loop 1 fixes and hunting regressions introduced by the fixes

---

## Loop 1 Finding Resolutions

### COR-01: RESOLVED — readText() before waitFor() dead-code timeout

`CompletableFuture.supplyAsync { result.inputStream.bufferedReader().readText() }` is now launched at `SpikeDecisionDocument.kt:26` before `result.waitFor(30, TimeUnit.SECONDS)` at line 27. The background thread reads stdout concurrently while the main thread blocks on the timeout. The ordering is correct: future starts → waitFor blocks → on timeout: destroyForcibly + cancel(true) + return false (no .get() after cancel, so CancellationException is avoided). The pattern mirrors the existing `CompletableFuture.supplyAsync` pattern in SpikeShader1DecisionSpec.kt:114.

**One residual edge case (LOW, new):** See COR-L2-02 below.

---

### COR-02: RESOLVED (directory-absent path)

`val parent = path.parent ?: return false` at line 19 null-guards the bare-filename case. `if (!parent.toFile().exists()) return false` at line 20 guards the missing-directory case. TC-29 in `SpikeDecisionDocumentEdgeCaseSpec.kt` verifies both paths. The `IOException`-from-missing-directory failure path is closed.

**One residual failure path (MEDIUM, new):** See COR-L2-01 below — `IOException` from binary-not-found is still uncaught.

---

### COR-04: RESOLVED — findLibrary().get() NoSuchElementException

Both convention plugins now use `.orElseThrow { GradleException("Catalog entry '...' not found in libs catalog") }`:
- `khaos.kotlin-kmp.gradle.kts:19-20`
- `khaos.kotlin-jvm.gradle.kts:16-17`

`NoSuchElementException` is replaced with a named, actionable `GradleException` that surfaces at configuration time with the missing entry's name. Fix is correct in both files.

---

### COR-05: RESOLVED — jacocoTestReport runs after failed test

`finalizedBy(tasks.jacocoTestReport)` is removed from `tasks.test` in `khaos.kotlin-jvm.gradle.kts`. `tasks.jacocoTestReport` now uses `dependsOn(tasks.test)`. Under this wiring `./gradlew test` does not invoke `jacocoTestReport`; `./gradlew test jacocoTestReport` (or `./gradlew jacocoTestReport`) runs both in dependency order and only on success of test. The spurious JaCoCo noise on test failure is eliminated.

**Residual observation (carried by Paleontologist as PAL-L2-01/PAL-L2-03):** `./gradlew test` alone produces no coverage report. TC-30 passes vacuously by absence of `finalizedBy`. This is a test-plan gap, not a failure path introduced by the fix; it does not produce incorrect behavior, only a silent omission. Not escalated here.

---

### COR-06: NOT FIXED — KSP version "2.3.6" remains

`gradle/libs.versions.toml:3` still reads `ksp = "2.3.6"`. The fix commit did not touch this line. The KSP versioning scheme for Kotlin 2.x is `<kotlinVersion>-<kspRelease>` (e.g., `2.3.20-1.0.25`). Version `2.3.6` does not exist in the KSP release index. Since `ksp` is declared `apply false` in root and no module applies it yet, build resolution has not exploded — but the phantom version record persists.

**Status:** OPEN — carried forward unchanged.

---

### COR-11: RESOLVED — KMP modules lack JaCoCo TODO

`khaos.kotlin-kmp.gradle.kts:1` now reads:
```
// TODO(issue-N): wire JaCoCo for jvmTest task in KMP modules — deferred from Issue #3 (design A6)
```
The comment names `jvmTest` and references a deferral placeholder. TC-27 was added to the test plan asserting its presence. The deferral is now tracked in code.

**Residual observation (carried by Paleontologist as PAL-L2-04):** `issue-N` is a non-resolvable placeholder; it should be replaced with the actual GitHub issue number (`#4` or a new issue) before merge. Not a failure path, but a maintainability gap.

---

### COR-03, COR-07, COR-08, COR-09, COR-10

These lower-severity findings were not targeted for loop 1 remediation. No regression introduced against them by the fix commit.

---

## New Findings — Loop 2

### [MEDIUM] COR-L2-01: IOException from pb.start() uncaught when git binary not found after env.clear()
**Reviewer:** The Coroner (failure-path analysis)
**Location:** `khaos-test-harness/src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:24-25`
**Category:** exception / contract

The COR-02 fix correctly guards against `path.parent == null` and against a missing parent directory (both return `false`). However, a new failure path was introduced by the INF-01 fix (`pb.environment().clear()` at line 24).

After `clear()`, the child process environment has no `PATH` variable. `ProcessBuilder.start()` calls the JVM native layer which invokes `execvp("git", ...)`. The `execvp` implementation in glibc and macOS libc falls back to a hardcoded default path (`/usr/bin:/bin` on Linux, `/usr/bin:/bin:/usr/sbin:/sbin` on macOS) when `PATH` is absent. This works only if `git` is installed at one of these default locations. On many CI runners and developer machines, git is installed at `/usr/local/bin/git` (Homebrew, GitHub Actions macOS runner) or `/opt/homebrew/bin/git` — neither of which is in the default fallback.

When `execvp` cannot locate the binary, the JVM throws `IOException: error=2, No such file or directory`. This `IOException` propagates **uncaught** from `committedToGit()`, violating the method's boolean contract. The caller receives a JVM exception stack trace instead of `false`.

**Contrast with SpikeShader1DecisionSpec.kt:107-111**, which explicitly restores `PATH` after clearing:
```kotlin
pb.environment().also {
    it.clear()
    it["PATH"] = System.getenv("PATH") ?: "/usr/bin:/bin"
    it["HOME"]  = System.getenv("HOME")  ?: ""
    it["GH_TOKEN"] = token
}
```
`SpikeDecisionDocument.kt` clears the environment but does **not** restore `PATH`:
```kotlin
pb.environment().clear()    // line 24 — PATH is now absent
val result = pb.start()     // line 25 — IOException if git not at /usr/bin/git
```

**Evidence:** `SpikeDecisionDocument.kt:24-25`. No `try { } catch (e: IOException) { return false }` wrapping `pb.start()`. No `pb.environment()["PATH"] = ...` after `clear()`. TC-29 does not cover this failure path (it only exercises null-parent and nonexistent-directory).

**Impact:** On GitHub Actions macOS runners and any system where git is Homebrew-installed, `committedToGit()` throws `IOException` and crashes the test thread with a JVM exception rather than returning `false`. This is an operational regression introduced by the security fix.

**Fix:** Either (a) restore `PATH` and `HOME` after `clear()` matching the SpikeShader pattern, or (b) wrap `pb.start()` in `try { } catch (e: IOException) { return false }`, or both.

---

### [LOW] COR-L2-02: outputFuture leaked on exitValue() != 0 branch
**Reviewer:** The Coroner (failure-path analysis)
**Location:** `khaos-test-harness/src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:33`
**Category:** concurrency / resource

When `git log` exits with a non-zero exit code (corrupted repo, permission error), line 33 executes `if (result.exitValue() != 0) return false`. The `outputFuture` background thread is neither cancelled nor joined before this return. The CompletableFuture continues reading from the process `inputStream` on the ForkJoinPool worker thread until the stream reaches EOF — which happens quickly since the process has already exited — but no explicit cleanup occurs.

This is a thread-hygiene issue rather than a correctness bug: the future completes naturally within milliseconds and the FJP thread is returned to the pool. In practice, the `git log` output for a non-zero exit is small and the stream drains fast. The risk is low under normal test execution.

**Evidence:**
```kotlin
val exited = result.waitFor(30, TimeUnit.SECONDS)
if (!exited) {
    result.destroyForcibly()
    outputFuture.cancel(true)   // ← correctly cancelled on timeout
    return false
}
if (result.exitValue() != 0) return false   // ← future NOT cancelled here
val output = outputFuture.get(5, TimeUnit.SECONDS)
```

**Contrast:** The timeout branch (lines 29-31) correctly calls `outputFuture.cancel(true)`. The non-zero-exit branch (line 33) does not. The asymmetry is inconsistent even if the practical impact is low.

**Fix (optional):** Add `outputFuture.cancel(true)` before the `return false` on line 33, matching the timeout-branch pattern.

---

## Summary Table

| ID | Loop 1 ID | Severity | Title | Status |
|---|---|---|---|---|
| — | COR-01 | HIGH | readText() before waitFor() dead-code timeout | **RESOLVED** |
| — | COR-02 | MEDIUM | IOException from missing parent directory | **RESOLVED** |
| — | COR-04 | HIGH | findLibrary().get() NoSuchElementException | **RESOLVED** |
| — | COR-05 | MEDIUM | jacocoTestReport runs after failed test | **RESOLVED** |
| — | COR-06 | HIGH | KSP 2.3.6 phantom version | **NOT FIXED** |
| — | COR-11 | MEDIUM | KMP modules lack JaCoCo wiring/TODO | **RESOLVED** |
| COR-L2-01 | *(new)* | MEDIUM | IOException uncaught when git binary absent after env.clear() | **NEW FINDING** |
| COR-L2-02 | *(new)* | LOW | outputFuture leaked on exitValue() != 0 branch | **NEW FINDING** |

---

## Verdict: REQUEST CHANGES

**Blocking (must fix before merge):**

- **COR-L2-01 [MEDIUM]:** The `pb.environment().clear()` fix (INF-01 security fix) introduced a new failure path: `pb.start()` throws uncaught `IOException` on any system where `git` is not at `/usr/bin/git` or `/bin/git` (Homebrew macOS runners, custom CI environments). The `committedToGit()` boolean contract is broken for this case. Restore `PATH` after clearing (matching the SpikeShader pattern) or wrap `pb.start()` in a caught `IOException` that returns `false`.

- **COR-06 [HIGH, carried]:** `ksp = "2.3.6"` remains a phantom version that does not exist in the KSP release index. Not introduced by loop 2 fixes, but the risk window grows with each passing loop. Correct to the appropriate KSP format for Kotlin 2.3.x (e.g., `2.3.20-1.0.25` or the nearest valid release).

**Non-blocking, recommended:**

- **COR-L2-02 [LOW]:** Add `outputFuture.cancel(true)` before the `exitValue() != 0` early return to match the cleanup pattern in the timeout branch. Low risk in practice but removes an asymmetric resource leak.
