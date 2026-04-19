# Gauntlet Loop 2 — Prosecutor Re-Review
# Issue #3: F-1 Gradle Multi-Module Project Scaffolding

**Date:** 2026-04-19
**PR:** #37
**Branch:** `feature/issue-3-gradle-multi-module-scaffolding`
**Reviewer:** The Prosecutor (Spec Compliance)
**Loop:** 2 — verifying Loop 1 findings

---

## Loop 1 Finding Re-Assessments

### PRO-01: RESOLVED — with scope caveat (see PRO-NEW-01 below)

**Confirmation:** `gradle/libs.versions.toml` now contains a multi-line comment above the four non-native LWJGL entries explaining the inline declaration strategy:

```toml
# Native classifier variants (natives-linux, natives-macos-arm64, natives-windows, etc.)
# are NOT expressible in TOML (Gradle 9.x does not support `classifier =` in catalog entries).
# Declare natives inline at the module level using one of:
#   variantOf(libs.lwjgl.core) { classifier("natives-linux") }
#   "org.lwjgl:lwjgl::natives-linux"
#   ...
# See D6 in planning/designs/issue-3-design.md for the full native strategy.
```

The Sentinel's revised TC-28 accepts a TOML comment as sufficient to meet AC #2's native strategy requirement. The comment is present and explicit. **PRO-01 is RESOLVED as a classifier-absence finding.** The underlying design doc deviation is captured as PRO-NEW-01.

---

### PRO-02: RESOLVED

**Confirmation:** TC-02 on the PR branch (`planning/test-plans/issue-3-plan.md`, line 27–40) has been fully rewritten. The classifier-based assertions for `lwjgl-natives-linux`, `lwjgl-natives-linux-arm64`, `lwjgl-natives-macos`, `lwjgl-natives-macos-arm64`, `lwjgl-natives-windows`, `lwjgl-vulkan-natives-macos`, `lwjgl-vulkan-natives-macos-arm64` are removed. TC-02 now asserts the four non-native LWJGL catalog entries and explicitly verifies that a `classifier =` absence and inline-strategy comment are present. TC-02 can pass against the current implementation.

---

### PRO-04: PARTIALLY RESOLVED — test plan not updated (see PRO-NEW-02)

**Partial confirmation:** `build.gradle.kts` now contains a `NOTE` comment acknowledging the buildSrc classpath conflict and labelling the omission a "D5 approved deviation":

```kotlin
// NOTE: alias(libs.plugins.kotlin.multiplatform) and alias(libs.plugins.kotlin.jvm) cannot be
// declared here — buildSrc puts kotlin-gradle-plugin on the classpath and Gradle rejects a
// re-declaration ("already on classpath with unknown version"). D5 approved deviation.
// See _bmad/memory/agent-dev/MEMORY.md: "Kotlin plugin + buildSrc classpath conflict".
```

The implementation acknowledges the constraint. However, two defects remain:

1. TC-16 and TC-19 in `planning/test-plans/issue-3-plan.md` still read `[REQUIRES RESOLUTION]` for both Kotlin plugin alias assertions. They were never updated to state the deviation is documented, nor to convert the failing assertion into a deviation-verification assertion. A test plan with unresolved `[REQUIRES RESOLUTION]` markers is unexecutable on those items.

2. `planning/designs/issue-3-design.md` is **not on the PR branch** (`fatal: path 'planning/designs/issue-3-design.md' exists on disk, but not in 'origin/feature/issue-3-gradle-multi-module-scaffolding'`). The "D5 approved deviation" referenced in the build file comment has no corresponding update in the design document itself. D5's canonical text still specifies three plugin aliases. This is a broken documentation chain: the build file claims a design-level approval that the design document does not contain.

**PRO-04 is PARTIALLY RESOLVED** — the code acknowledges the constraint but the test plan retains unresolvable assertions and the design doc is absent from the branch.

---

### PRO-05: RESOLVED

**Confirmation:** Same as PRO-01 assessment. The TOML comment explicitly names `variantOf()` and `::cls` inline notation. TC-28 accepts this as the mechanism. No module build file is required by TC-28 (option (a) is sufficient).

---

### PRO-06: RESOLVED

**Confirmation:** TC-03, TC-04, and TC-12 on the PR branch now correctly reference `khaos.kotlin-kmp.gradle.kts` and `khaos.kotlin-jvm.gradle.kts` (with the mandatory `khaos.` prefix). The implementation note in TC-12 explicitly explains that the prefix is mandatory for Gradle plugin ID derivation. All three TCs can pass against the current implementation.

---

### PRO-08: NOT RESOLVED

**Confirmation:** The `_bmad/memory/agent-dev/` files are still present in the PR diff:

```
_bmad/memory/agent-dev/BOND.md
_bmad/memory/agent-dev/CAPABILITIES.md
_bmad/memory/agent-dev/CREED.md
_bmad/memory/agent-dev/INDEX.md
_bmad/memory/agent-dev/MEMORY.md
_bmad/memory/agent-dev/PERSONA.md
_bmad/memory/agent-dev/sessions/2026-04-18.md
```

Seven agent persona/memory files remain in this scaffolding PR. No AC item, TC, or design decision references or permits them. The scope-creep finding stands.

---

## New Findings — Loop 2

### [HIGH] TC-16 and TC-19 retain unresolvable `[REQUIRES RESOLUTION]` markers — test plan is unexecutable on D5 assertions
**Reviewer:** The Prosecutor (Spec Compliance)
**Location:** `planning/test-plans/issue-3-plan.md:162-164` (TC-16), `planning/test-plans/issue-3-plan.md:193-197` (TC-19)
**Category:** test-gap

TC-16 contains two bullet points marked `[REQUIRES RESOLUTION]` requiring `alias(libs.plugins.kotlin.multiplatform) apply false` and `alias(libs.plugins.kotlin.jvm) apply false` in the root build file. TC-19 repeats the same two assertions with the same marker. The resolution note in TC-19 states: "this TC must be annotated as a known deviation from D5: document the buildSrc classpath constraint as an approved exception in the design doc and update this assertion to verify the deviation is documented rather than failing the implementation." Neither action was taken. The markers remain verbatim on the PR branch.

Any executor of TC-16 or TC-19 encounters two assertions that (a) cannot pass against the implementation and (b) were never resolved to a definitive pass/fail/deviation state. A `[REQUIRES RESOLUTION]` marker in a merged test plan is a process failure — the plan cannot be executed mechanically.

**Evidence:** `git show origin/feature/issue-3-gradle-multi-module-scaffolding:planning/test-plans/issue-3-plan.md` — TC-16 lines 162–164 and TC-19 lines 193–197 contain `[REQUIRES RESOLUTION]` verbatim. The build.gradle.kts comment says "D5 approved deviation" but the test plan never received the corresponding annotation update.

**Spec reference:** TC-16; TC-19; design D5

---

### [HIGH] Design doc (`planning/designs/issue-3-design.md`) absent from PR branch — "D5 approved deviation" is a dangling reference
**Reviewer:** The Prosecutor (Spec Compliance)
**Location:** `build.gradle.kts:1-3`; `planning/designs/issue-3-design.md` (absent)
**Category:** design-deviation

The root `build.gradle.kts` comment states "D5 approved deviation" and cites `_bmad/memory/agent-dev/MEMORY.md`. D5 in `planning/designs/issue-3-design.md` still specifies three `alias(...)` entries including both Kotlin plugin aliases. The design document was not modified in this PR (`fatal: path 'planning/designs/issue-3-design.md' exists on disk, but not in 'origin/feature/issue-3-gradle-multi-module-scaffolding'`). The claimed "approval" exists only in a build file comment citing an agent memory file — not in the authoritative design document. A future reviewer reading D5 will see the original three-alias specification and no record of why the implementation omits two of them.

**Evidence:** `build.gradle.kts` line 4: `// D5 approved deviation.` — `planning/designs/issue-3-design.md` is absent from the PR branch; local D5 still reads `alias(libs.plugins.kotlin.multiplatform) apply false / alias(libs.plugins.kotlin.jvm) apply false / alias(libs.plugins.ksp) apply false`.

**Spec reference:** Design D5; TC-16; TC-19

---

### [MEDIUM] TC-30 formally satisfied but D3's `finalizedBy` removed without documentation — JaCoCo auto-run silently dropped
**Reviewer:** The Prosecutor (Spec Compliance)
**Location:** `buildSrc/src/main/kotlin/khaos.kotlin-jvm.gradle.kts:20-27`
**Category:** design-deviation

TC-30 required that `jacocoTestReport` not run unconditionally on test failure (the "unacceptable" pattern was bare `finalizedBy` with no guard). The implementation resolves TC-30 by removing `finalizedBy` entirely. TC-30 technically passes: there is no bare `finalizedBy`. However, design D3 explicitly specified `tasks.test { useJUnitPlatform(); finalizedBy(tasks.jacocoTestReport) }`. The implemented convention plugin has:

```kotlin
tasks.test {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    ...
}
```

`jacocoTestReport` now requires explicit invocation — it will not auto-run after a successful test run. Running `./gradlew :khaos-test-harness:test` will not produce a coverage report; `./gradlew :khaos-test-harness:jacocoTestReport` must be called separately. This is a functional regression from D3's intent (automatic coverage after test pass) with no documented deviation in the design doc or test plan. TC-30's acceptable pattern listed `onlyIf { tasks.test.get().state.failure == null }` — a guarded `finalizedBy` — not removal. The implementation chose a more restrictive path (no auto-run at all) without noting the departure.

**Evidence:** D3 code block: `tasks.test { useJUnitPlatform(); finalizedBy(tasks.jacocoTestReport) }`. Implemented `khaos.kotlin-jvm.gradle.kts`: `tasks.test { useJUnitPlatform() }` — no `finalizedBy`. `tasks.jacocoTestReport { dependsOn(tasks.test) }` — coverage report is manual-only. Design doc absent from PR branch; no deviation note in test plan.

**Spec reference:** Design D3; TC-30

---

### [MEDIUM] `_bmad/memory/agent-dev/` scope creep not remediated
**Reviewer:** The Prosecutor (Spec Compliance)
**Location:** `_bmad/memory/agent-dev/` (7 files)
**Category:** scope-creep

PRO-08 from Loop 1 is reproduced verbatim. Seven agent persona/memory files remain in the PR diff. These are unrelated to Gradle scaffolding and have no AC, TC, or design reference. The finding was not addressed between loops.

**Evidence:** `gh pr diff 37 --name-only` output includes `_bmad/memory/agent-dev/BOND.md`, `CAPABILITIES.md`, `CREED.md`, `INDEX.md`, `MEMORY.md`, `PERSONA.md`, `sessions/2026-04-18.md`.

**Spec reference:** Issue #3 AC (all items); no TC or design decision covers these files

---

## Summary Table

| Finding | Severity | Status |
|---|---|---|
| PRO-01 — LWJGL classifier entries stripped | CRITICAL | RESOLVED (comment + TC-28 satisfied) |
| PRO-02 — TC-02 cannot pass (6 missing entries) | CRITICAL | RESOLVED |
| PRO-04 — D5 violated (Kotlin aliases absent) | HIGH | PARTIALLY RESOLVED — code comment present; TC-16/TC-19 not updated; design doc absent from branch |
| PRO-05 — No alternative native declaration mechanism | HIGH | RESOLVED (TOML comment satisfies TC-28) |
| PRO-06 — TC-12/TC-03/TC-04 wrong file names | MEDIUM | RESOLVED |
| PRO-08 — `_bmad` scope creep | MEDIUM | NOT RESOLVED |
| PRO-NEW-01 — TC-16/TC-19 retain `[REQUIRES RESOLUTION]` | HIGH | NEW |
| PRO-NEW-02 — Design doc absent from PR; D5 deviation undocumented | HIGH | NEW |
| PRO-NEW-03 — D3 `finalizedBy` removed without deviation note | MEDIUM | NEW |
| PRO-NEW-04 — `_bmad` scope creep (reproduction of PRO-08) | MEDIUM | NOT RESOLVED |

**Critical findings:** 0 remaining
**High findings unresolved:** 3 (PRO-04 partial + PRO-NEW-01 + PRO-NEW-02)
**Medium findings unresolved:** 2 (PRO-08 / PRO-NEW-04 are the same; PRO-NEW-03)

---

## Verdict: REQUEST CHANGES

**Rationale:**

The two Critical findings from Loop 1 (PRO-01, PRO-02) are resolved. The core implementation — module structure, convention plugins, catalog, wrapper SHA, group ID, env sanitization, timeout fix, Optional guards, edge-case spec — is materially improved and the loop 2 infiltrator/paleontologist review should confirm those fixes.

However, three High/Medium documentation and test-plan defects block clean merge:

1. **TC-16 and TC-19 retain `[REQUIRES RESOLUTION]`** (PRO-NEW-01). These are unexecutable assertions in a merged test plan. The deviation must be declared — convert each `[REQUIRES RESOLUTION]` bullet to a passing assertion ("verified that the deviation is documented in the build file comment and design doc") or remove the bullet if the assertion is moot.

2. **Design doc absent from the PR branch** (PRO-NEW-02). `planning/designs/issue-3-design.md` must be added to the branch with D5 annotated to acknowledge the buildSrc classpath conflict as a documented exception. A build file comment citing an agent MEMORY.md is not a substitute for updating the design record.

3. **`_bmad/memory/agent-dev/` scope creep** (PRO-08, NOT RESOLVED). These seven files must be removed from the PR diff. Agent persona artifacts do not belong in a Gradle scaffolding commit.

PRO-NEW-03 (jacocoTestReport deviation) is Medium — it does not block merge if the two High items are resolved and a brief deviation note is added to the convention plugin or design doc.

Recommended minimum fix set:
- Remove `_bmad/memory/agent-dev/` files from the PR (squash or `git rm` + new commit)
- Add `planning/designs/issue-3-design.md` to the branch with D5 annotated
- Update TC-16 and TC-19 to resolve the `[REQUIRES RESOLUTION]` markers (convert to deviation-verified assertions or annotate as "deviation documented at `build.gradle.kts:1`")
- Add a one-line deviation note to `khaos.kotlin-jvm.gradle.kts` explaining why `finalizedBy` was removed (Medium, advisable)
