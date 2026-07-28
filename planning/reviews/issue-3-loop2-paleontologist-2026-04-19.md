# Gauntlet Loop 2 — Paleontologist Review
## Issue #3: F-1 Gradle Multi-Module Project Scaffolding

**Reviewer:** The Paleontologist (historical/maintainability)
**Date:** 2026-04-19
**PR:** #37 — branch `feature/issue-3-gradle-multi-module-scaffolding`
**Loop:** 2 (re-review after Dev addressed Loop 1 findings)

---

## Loop 1 Finding Resolutions

### PAL-01: RESOLVED — TC-02 no longer asserts classifier entries

TC-02 has been completely rewritten. The new text explicitly acknowledges that Gradle 9.x TOML does not support `classifier =` entries, removes all seven classifier-entry assertions, and instead asserts (a) the four non-native LWJGL entries are present, (b) no `classifier =` appears, and (c) a comment explaining the inline declaration strategy is present in the file. `libs.versions.toml` contains exactly this comment block at lines 9–17. The test plan and the implementation are now in agreement.

### PAL-02: RESOLVED — TC-03, TC-04, TC-12 now reference `khaos.kotlin-*.gradle.kts`

All three TCs have been updated. TC-03 now asserts `buildSrc/src/main/kotlin/khaos.kotlin-kmp.gradle.kts`. TC-04 now asserts `buildSrc/src/main/kotlin/khaos.kotlin-jvm.gradle.kts`. TC-12 references both with the `khaos.` prefix and includes an implementation note explaining why the prefix is mandatory for Gradle plugin ID derivation. The files confirmed to exist on disk match the TCs exactly.

### PAL-03: PARTIALLY RESOLVED — KSP version 2.3.6 still unresolvable; TC-24 added but does not fix the version

`gradle/libs.versions.toml` line 3 still reads `ksp = "2.3.6"`. TC-24 was added to the test plan and explicitly flags this: "Note: KSP versions for Kotlin 2.x historically follow the `<kotlinVersion>-<kspRelease>` format … verify this is true before accepting a standalone version string like `2.3.6`. If `2.3.6` does not exist on any registry, correct to the appropriate format." The TC correctly describes the risk but the version in the catalog was not corrected. The finding remains open as a deferred validation item rather than a resolved one. See new finding PAL-03-L2 below.

### PAL-04: RESOLVED — `group = "dev.khaos"` declared via `allprojects` in root

`build.gradle.kts` contains:
```kotlin
allprojects {
    group = "dev.khaos"
}
```
TC-26 was added to assert this. The AC requirement "Group ID: dev.khaos" is now met. TC-26's verification instruction ("static file content check across root and/or submodule build files") is accurate.

### PAL-05: RESOLVED — JaCoCo deferral TODO present in KMP convention plugin

`khaos.kotlin-kmp.gradle.kts` line 1:
```
// TODO(issue-N): wire JaCoCo for jvmTest task in KMP modules — deferred from Issue #3 (design A6)
```
TC-27 was added requiring this comment. Both the code and the TC are satisfied. One residual observation is noted below (PAL-11-L2) regarding the unresolved `issue-N` placeholder.

### PAL-06: ACCEPTABLE — workingDir/user.dir coupling acknowledged; no change required

No change was made to this pattern. TC-25 was added requiring that `khaos-test-harness/build.gradle.kts` sets `workingDir = rootProject.projectDir`, which it does (`khaos-test-harness/build.gradle.kts` line 4). The finding was tagged "acceptable as-is" in Loop 1. The TC formalizes the requirement. Confirmed stable.

### PAL-07/08/09/10: ACCEPTED AS-IS — no changes; remain low-priority open items

No action was taken on these four low-severity findings. They remain in the same state as Loop 1. No regression introduced.

---

## New Findings — Loop 2

### [HIGH] TC-30 asserts a guard that does not exist in the implementation
**Reviewer:** The Paleontologist (historical/maintainability)
**Location:** `planning/test-plans/issue-3-plan.md:TC-30` vs `buildSrc/src/main/kotlin/khaos.kotlin-jvm.gradle.kts:20-30`
**Category:** test-debt

TC-30 was added in Loop 1's shadow patch to address the Coroner's finding that `jacocoTestReport` runs unconditionally after a failed test task. TC-30 states:

> Acceptable patterns: `jacocoTestReport { onlyIf { tasks.test.get().state.failure == null } }` or equivalent conditional wiring
> Unacceptable: bare `finalizedBy(tasks.jacocoTestReport)` with no guard — `finalizedBy` fires even when the test task fails before producing an `.exec` file

The actual `khaos.kotlin-jvm.gradle.kts` as implemented in Loop 2 contains:

```kotlin
tasks.test {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports { ... }
}
```

There is no `finalizedBy` call (good), but there is also no `onlyIf` guard, no conditional wiring, and — critically — `jacocoTestReport` is configured with `dependsOn(tasks.test)` rather than being triggered by the test task. This means `jacocoTestReport` does **not** run automatically after `./gradlew test`; it must be invoked explicitly. TC-30 asserts that `jacocoTestReport` does not run unconditionally on test failure — which is technically true under this implementation because it is not triggered at all by default, but for the wrong reason. The implementation has traded one failure mode (spurious JaCoCo run on failure) for a different gap: the coverage report is now silently skipped unless explicitly requested.

TC-30's acceptance criterion tests for `onlyIf` wiring or conditional triggering. The implementation has neither. A reviewer mechanically applying TC-30 would scan for `onlyIf` or `finalizedBy` — finding neither — and face an ambiguous result: did the TC pass (no spurious run) or fail (no trigger at all)?

**Evidence:**
- `khaos.kotlin-jvm.gradle.kts:20-30` — `tasks.test` has only `useJUnitPlatform()`; `tasks.jacocoTestReport` uses `dependsOn`, not `finalizedBy` or `onlyIf`.
- `planning/test-plans/issue-3-plan.md:TC-30` — asserts "acceptable patterns: `jacocoTestReport { onlyIf { ... } }`" but neither pattern is present.
- The design doc (`planning/designs/issue-3-design.md:151`) originally had `finalizedBy(tasks.jacocoTestReport)` inside `tasks.test` — the Dev removed `finalizedBy` but did not add the `onlyIf` guard the TC demands.

**Maintainability cost:** The next time someone runs `./gradlew :khaos-test-harness:test`, no coverage report is generated. Shift-left-dev's coverage gate will fail silently or skip. TC-30 provides false assurance that the coverage trigger problem was resolved; it was not — it was replaced with a different architectural gap. Future engineers will not know whether the absence of `finalizedBy` was intentional or an oversight, because the commit message does not explain the choice and TC-30 only describes what is NOT acceptable, not what the chosen pattern is.

---

### [MEDIUM] PAL-03-L2: KSP version `2.3.6` still in catalog after two loops — TC-24 defers rather than resolves
**Reviewer:** The Paleontologist (historical/maintainability)
**Location:** `gradle/libs.versions.toml:3`
**Category:** maintainability

After two loops, `ksp = "2.3.6"` remains unchanged in the catalog. TC-24 was added in Loop 1's shadow patch and correctly documents the risk, but it adds the verification step as a TODO for the developer running the tests rather than fixing the version in the catalog. TC-24's note reads: "If `2.3.6` does not exist on any registry, correct to the appropriate format per KSP release notes." No correction has been made.

KSP's published releases for Kotlin 2.x follow the `<kotlinVersion>-<kspRelease>` scheme. The artifact `com.google.devtools.ksp:symbol-processing-gradle-plugin:2.3.6` does not correspond to any known KSP release. The likely correct version for Kotlin 2.3.20 would be `2.3.20-1.0.31` or the equivalent current patch. This remains a latent build failure: the moment any module declares `alias(libs.plugins.ksp)`, the build will fail at Gradle Plugin Portal resolution with a "not found" error.

**Evidence:** `gradle/libs.versions.toml:3` — `ksp = "2.3.6"` unchanged from Loop 1. TC-24 added but treats verification as developer action, not a fix requirement.

**Maintainability cost:** Two loops have passed without correction. The version is now a known-bad value that exists in the test plan as a deferred verification item. The longer this sits, the higher the probability that a future agent or developer treats it as a known-good value (since it survived two Gauntlet loops) and applies KSP without checking, triggering an immediate resolution failure.

---

### [MEDIUM] TC-30 acceptance criterion is ambiguous — "no finalizedBy" and "has onlyIf" are not the same gate
**Reviewer:** The Paleontologist (historical/maintainability)
**Location:** `planning/test-plans/issue-3-plan.md:TC-30`
**Category:** test-debt

TC-30 is written to guard against `finalizedBy` with no guard. The implementation has neither `finalizedBy` nor `onlyIf`. TC-30 as written does not cover the case where `jacocoTestReport` is not wired to the test lifecycle at all. This creates a vacuously passing assertion: "jacocoTestReport does not run unconditionally" is true if it never runs. TC-30 should be updated to assert one of:

- (a) `finalizedBy` with `onlyIf` — conditional automatic trigger, OR
- (b) An explicit statement that `jacocoTestReport` must be run explicitly and that this is the intentional design, OR
- (c) A specific `./gradlew :khaos-test-harness:test jacocoTestReport` invocation in the verification step

As written, a static code inspector checking for the absence of bare `finalizedBy` will pass TC-30 against the current implementation, even though the coverage report is never generated during normal `./gradlew test` execution.

**Evidence:** TC-30 verification instruction: "static code inspection of `khaos.kotlin-jvm.gradle.kts` test task configuration." The test task configuration contains only `useJUnitPlatform()`. There is nothing to inspect for TC-30 compliance — the TC passes by absence.

---

### [MEDIUM] PAL-11-L2: JaCoCo TODO uses `issue-N` placeholder — deferral has no owner or tracking link
**Reviewer:** The Paleontologist (historical/maintainability)
**Location:** `buildSrc/src/main/kotlin/khaos.kotlin-kmp.gradle.kts:1`
**Category:** test-debt

The JaCoCo TODO comment reads:
```
// TODO(issue-N): wire JaCoCo for jvmTest task in KMP modules — deferred from Issue #3 (design A6)
```

TC-27 requires "a TODO comment referencing the JaCoCo wiring deferral and the issue that will address it (acceptable: `// TODO(issue-N): wire JaCoCo for jvmTest task in KMP modules`)." The TC explicitly accepts `issue-N` as the placeholder. However, `issue-N` is not a resolvable reference — it is a template fragment. The design doc (A6 in `issue-3-design.md`) says "deferred to Issue #4" in prose but the code says `issue-N`. These are inconsistent and neither is a GitHub issue link.

If the project uses GitHub Issues, the TODO should reference `#4` or the actual issue number for KMP coverage wiring. If no such issue exists, the TODO is a placeholder that will never be resolved — it will persist as archaeology in the file forever.

**Evidence:** `khaos.kotlin-kmp.gradle.kts:1` — `TODO(issue-N)`. Design `issue-3-design.md:A6` — "defer JaCoCo integration to a later issue (or to the coverage tool used by shift-left-dev) ... Recommend flagging this as a follow-on task when Issue #4 (first KMP module implementation) approaches review." The design references Issue #4 but the code says `issue-N`.

**Maintainability cost:** Low immediate cost, but the standard for tracked TODOs is that `issue-N` is resolved before merge. A TODO pointing to a real issue number enables the GitHub search `TODO(#4)` to surface all relevant locations when that issue is worked. `TODO(issue-N)` is unsearchable and will accumulate.

---

### [LOW] `SpikeDecisionDocumentEdgeCaseSpec` follows project conventions correctly — no finding
**Reviewer:** The Paleontologist (historical/maintainability)
**Location:** `khaos-test-harness/src/test/kotlin/khaos/spike/SpikeDecisionDocumentEdgeCaseSpec.kt`
**Category:** observation (no action required)

The new spec file uses `ShouldSpec`, `withClue`, and package `khaos.spike` — consistent with the existing spike specs. It covers both TC-29 edge cases (null parent, non-existent parent directory) in separate `should()` blocks with descriptive clue strings. The two test methods are independent and deterministic. No maintainability concern introduced.

Note: the `khaos.spike` package divergence from `dev.khaos.*` (PAL-09 from Loop 1) is carried through consistently in this file. The divergence pre-dates Loop 2 and was accepted as-is in Loop 1; it is not a new regression.

---

### [LOW] `committedToGit()` uses `path.parent` as git working directory — still semantically fragile post-fix
**Reviewer:** The Paleontologist (historical/maintainability)
**Location:** `khaos-test-harness/src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:22`
**Category:** coupling

The Loop 1 Coroner finding about `readText()` blocking before `waitFor()` was fixed correctly — the implementation now uses `CompletableFuture.supplyAsync` mirroring the spike spec pattern. However, a pre-existing structural choice remains: the git subprocess runs with `directory(parent.toFile())` rather than the project root.

Running `git log -- <absolute-path>` with working directory set to the file's parent is valid, but it means git discovers the repository root by walking upward from `parent`. For files deep in the project tree this is fine. For files in `planning/decisions/` (two levels below project root), git will correctly find the repo root. This is not a regression from Loop 1 — it was already the behavior — but it remains subtly different from the approach TC-20 and TC-25 use (workingDir = project root). The inconsistency between `committedToGit()`'s `path.parent` strategy and the spike specs' `System.getProperty("user.dir")` strategy means there are now two distinct "where is the repo root" conventions in this codebase. A future author adding a new helper in `SpikeDecisionDocument` may follow either convention and not know which is correct.

**Evidence:** `SpikeDecisionDocument.kt:22` — `.directory(parent.toFile())`. `SpikeShader1DecisionSpec.kt:12` — `Paths.get(System.getProperty("user.dir"))`. TC-20 requires `pb.environment().clear()` (addressed) but does not address the directory convention.

**Maintainability cost:** Low. Works correctly for all current file paths. Risk surfaces only if `SpikeDecisionDocument` is used with a path whose `parent` is in a different git worktree or nested submodule (PAL Coroner-LOW from Loop 1 still applies).

---

## Summary Table

| ID | Severity | Title | Status |
|---|---|---|---|
| PAL-01 | HIGH | TC-02 TOML classifier assertion | RESOLVED |
| PAL-02 | MEDIUM | TC-03/04/12 wrong file names | RESOLVED |
| PAL-03 | HIGH | KSP version 2.3.6 unresolvable | PARTIALLY RESOLVED (TC-24 added; version not corrected) |
| PAL-04 | MEDIUM | Group ID missing | RESOLVED (`allprojects { group = "dev.khaos" }` in root) |
| PAL-05 | MEDIUM | JaCoCo absent from KMP | RESOLVED (TODO comment + TC-27; deferral tracked) |
| PAL-06 | MEDIUM | workingDir/user.dir coupling | ACCEPTED (TC-25 formalizes requirement) |
| PAL-07–10 | LOW | Four low-friction items | ACCEPTED AS-IS |
| **PAL-L2-01** | **HIGH** | **TC-30 asserts guard that does not exist; coverage trigger absent** | NEW |
| **PAL-L2-02** | **MEDIUM** | **KSP 2.3.6 still in catalog after two loops** | NEW (escalation of PAL-03) |
| **PAL-L2-03** | **MEDIUM** | **TC-30 acceptance criterion vacuously passes by absence** | NEW |
| **PAL-L2-04** | **MEDIUM** | **JaCoCo TODO uses `issue-N` placeholder; not a real tracking reference** | NEW |
| PAL-L2-05 | LOW | `SpikeDecisionDocumentEdgeCaseSpec` follows conventions | OBSERVATION (no action) |
| PAL-L2-06 | LOW | `committedToGit()` `path.parent` vs project-root convention divergence | LOW (pre-existing, no regression) |

**New findings this loop:** 4 actionable (1 HIGH, 3 MEDIUM), 2 observations/LOW

---

## Verdict Recommendation

**REQUEST CHANGES**

The Paleontologist raises one blocking issue:

**PAL-L2-01 [HIGH]:** TC-30 was added in Loop 1 to fix the `finalizedBy`-with-no-guard problem. The Dev removed `finalizedBy` but added no automatic trigger for `jacocoTestReport`. The convention plugin now produces no coverage report on a plain `./gradlew test` run. TC-30 passes vacuously — it asserts the absence of `finalizedBy` but does not assert the presence of a working trigger. The coverage gate for `khaos-test-harness` is silently broken. This requires one of: (a) restoring `finalizedBy` with the `onlyIf` guard TC-30 prescribes, (b) updating TC-30 to explicitly accept the manual-invocation strategy and documenting why, or (c) adding a CI step that always runs `./gradlew test jacocoTestReport` sequentially.

**PAL-L2-02 and PAL-L2-03 [MEDIUM]:** The KSP version is a latent build failure that has now survived two Gauntlet loops without correction. TC-24 describes what must be verified but was not acted on. At a minimum, the correct version format should be established before this PR merges so it does not become a "known wrong" value that accumulates trust by age.

**PAL-L2-04 [MEDIUM]:** `TODO(issue-N)` is an unresolvable placeholder. Replace with `TODO(#N)` pointing to a real GitHub issue, or create the issue now and update the comment before merge.
