# Paleontologist Review — Issue #3: F-1 Gradle Multi-Module Scaffolding

**Reviewer:** paleontologist
**Date:** 2026-04-19
**Branch:** feature/issue-3-gradle-multi-module-scaffolding

---

```json
{
  "reviewer": "paleontologist",
  "findings": [
    {
      "id": "PAL-01",
      "title": "Test plan TC-02 asserts TOML classifier entries that Gradle 9.x cannot parse",
      "severity": "high",
      "category": "test-debt",
      "location": "planning/test-plans/issue-3-plan.md:TC-02",
      "description": "TC-02 requires lwjgl-natives-linux, lwjgl-natives-macos-arm64, and five more classifier entries in libs.versions.toml. Gradle 9.x TOML does not support the 'classifier' key in [libraries] — the build itself documents this (MEMORY.md, session log). The implementation correctly removed those entries and the design's fixup commit removed them too. TC-02 was never updated to reflect the change, so it now asserts a state that cannot exist in a Gradle 9.x build. Running TC-02 against the actual catalog will produce seven 'entry not found' failures immediately.",
      "evidence": "planning/test-plans/issue-3-plan.md lines 34–42 assert e.g. 'lwjgl-natives-linux (classifier natives-linux)'; gradle/libs.versions.toml has no classifier entries at all; commit 545566f message states 'libs.versions.toml: remove classifier entries (not supported in Gradle 9.x TOML)'",
      "future_cost": "The next agent or human who runs TC-02 mechanically finds seven failures and spends time diagnosing why before discovering the test plan is wrong, not the code. If the test plan is used as the coverage gate input for any later convention-plugin change, coverage will appear missing and the wrong thing gets re-added."
    },
    {
      "id": "PAL-02",
      "title": "Test plan TC-03, TC-04, TC-12 reference wrong file names for convention plugins",
      "severity": "medium",
      "category": "test-debt",
      "location": "planning/test-plans/issue-3-plan.md:TC-03,TC-04,TC-12",
      "description": "TC-03 checks 'buildSrc/src/main/kotlin/kotlin-kmp.gradle.kts'. TC-04 checks 'buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts'. TC-12 checks both. The actual files are 'khaos.kotlin-kmp.gradle.kts' and 'khaos.kotlin-jvm.gradle.kts' — Gradle requires the file name to match the plugin ID, so the 'khaos.' prefix is mandatory. The session log and MEMORY.md both document this gotcha explicitly. The test plan was written before the naming was discovered and was never corrected.",
      "evidence": "TC-03: 'buildSrc/src/main/kotlin/kotlin-kmp.gradle.kts'; actual path: 'buildSrc/src/main/kotlin/khaos.kotlin-kmp.gradle.kts'. Same for jvm variant. MEMORY.md: 'Convention plugin file names must match plugin ID: khaos.kotlin-kmp.gradle.kts not kotlin-kmp.gradle.kts'",
      "future_cost": "Any agent following the test plan for a regression check or coverage trace will verify the wrong paths, miss the real files, and either falsely pass (file-existence checks succeed on nothing) or spend cycles in confusion. The wrong names also appear in the design doc (D3), so the mismatch propagates to any documentation generated from the design."
    },
    {
      "id": "PAL-03",
      "title": "KSP version 2.3.6 is unresolvable when KSP is actually applied",
      "severity": "high",
      "category": "maintainability",
      "location": "gradle/libs.versions.toml:4",
      "description": "libs.versions.toml declares ksp = '2.3.6'. KSP 2.x has used the versioning convention 'kotlinVersion-kspPatch' (e.g. 2.3.20-1.0.31) throughout its lifecycle; no standalone '2.3.6' artifact exists on Maven Central or Gradle Plugin Portal. The design doc acknowledges this as assumption A2 ('verify compatibility with Kotlin 2.3.20 before implementation') and asserts 'KSP changed its versioning scheme at 2.3.x — it no longer embeds the Kotlin version.' That claim is not verified in any committed artifact or session log. The value currently in the catalog is never resolved because ksp is 'apply false' at root and applied in no submodule. The breakage is deferred, not fixed.",
      "evidence": "gradle/libs.versions.toml: ksp = '2.3.6' and kotlin = '2.3.20'. build.gradle.kts: 'alias(libs.plugins.ksp) apply false'. Design doc D4 KSP note: 'verify compatibility with Kotlin 2.3.20 before implementation'. No module applies KSP.",
      "future_cost": "The first issue that applies the KSP plugin to any module will fail at resolution time with a cryptic 'Could not find com.google.devtools.ksp:2.3.6' error. The developer will have to diagnose the versioning scheme, determine the correct string (e.g. '2.3.20-1.0.31'), and update the catalog. If the catalog update happens in a hurry under deadline pressure, other version pinning in the same file may be touched incorrectly."
    },
    {
      "id": "PAL-04",
      "title": "group ID missing from all submodule builds — AC stated 'dev.khaos' but not implemented",
      "severity": "medium",
      "category": "maintainability",
      "location": "khaos-core/build.gradle.kts, khaos-memory/build.gradle.kts, khaos-shader/build.gradle.kts, khaos-graph/build.gradle.kts, khaos-cmd/build.gradle.kts, khaos-test-harness/build.gradle.kts",
      "description": "The issue AC states 'Group ID: dev.khaos; artifact IDs match module names.' No submodule declares group = 'dev.khaos'. Gradle defaults the group to an empty string when unset. The design document never specifies where to put this declaration, and the test plan has no TC for it. For a hobby project with no near-term publishing intent, this is not a blocker, but it is a stated AC that was silently dropped.",
      "evidence": "grep for 'group' in all *.kts files returns zero results outside buildSrc build artifacts. Gradle's default group is \"\". Design document contains no mention of group ID configuration.",
      "future_cost": "When Maven Local or GitHub Packages publishing is added (required if any module is ever consumed as a dependency), every build.gradle.kts needs a group declaration or a centralized allprojects { group = 'dev.khaos' } block in root. The missing convention plugin pattern for this means each module will likely get it added inconsistently. If a future agent writes a TC for publishing, the AC gap will surface as a red test with no clear fix location."
    },
    {
      "id": "PAL-05",
      "title": "JaCoCo absent from all five KMP modules — coverage gate will never fire for their code",
      "severity": "medium",
      "category": "test-debt",
      "location": "buildSrc/src/main/kotlin/khaos.kotlin-kmp.gradle.kts",
      "description": "The khaos.kotlin-kmp convention plugin has no JaCoCo wiring. All five production modules (khaos-core, khaos-memory, khaos-shader, khaos-graph, khaos-cmd) apply this plugin. The shift-left-dev coverage gate runs diff-cover against a JaCoCo Cobertura XML. The design acknowledges this as A6 'deferred to Issue #4', but Issue #4 will implement the first KMP module — there is no dedicated issue to add KMP JaCoCo wiring. The deferral has no owner and no tracking. The khaos.kotlin-jvm plugin (applied only to khaos-test-harness) has JaCoCo wired correctly.",
      "evidence": "buildSrc/src/main/kotlin/khaos.kotlin-kmp.gradle.kts: no jacoco plugin, no jacocoTestReport task. buildSrc/src/main/kotlin/khaos.kotlin-jvm.gradle.kts: jacoco plugin applied, jacocoTestReport wired. Design doc A6: 'JaCoCo integration for KMP modules deferred to later issue'. No issue reference given.",
      "future_cost": "When any KMP module gets real implementation (Issue #4+), the coverage gate passes trivially because there is no JaCoCo report to diff against — diff-cover sees 0 lines covered and 0 lines changed in the report, which counts as 100% or is skipped depending on configuration. Bugs can land in KMP modules without triggering the coverage gate. Adding JaCoCo to KMP requires non-trivial task wiring (the jvmTest task, not the generic test task, needs instrumentation) and the convention plugin must be updated across all five modules simultaneously."
    },
    {
      "id": "PAL-06",
      "title": "workingDir / user.dir coupling between build file and test code is invisible and fragile",
      "severity": "medium",
      "category": "coupling",
      "location": "khaos-test-harness/build.gradle.kts:4, khaos-test-harness/src/test/kotlin/khaos/spike/SpikeShader1DecisionSpec.kt:12, khaos-test-harness/src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:11",
      "description": "Both spec files resolve the project root via Paths.get(System.getProperty('user.dir')). This works because khaos-test-harness/build.gradle.kts sets tasks.test { workingDir = rootProject.projectDir }. Gradle's Test task does propagate workingDir as the JVM's user.dir system property in the forked test process, so the contract is technically sound — but it is split across a build file and two separate test source files with no comment explaining the dependency. Running tests from IntelliJ IDEA (which forks tests with user.dir = the module directory, not the project root) breaks all path resolution silently: decisionDoc.exists returns false, TC-1 fails with a misleading 'file not found' message.",
      "evidence": "khaos-test-harness/build.gradle.kts line 4: 'workingDir = rootProject.projectDir'. SpikeShader1DecisionSpec.kt line 12: 'private val projectRoot = Paths.get(System.getProperty(\"user.dir\"))'. SpikeShader2DecisionSpec.kt line 11: same.",
      "future_cost": "Any developer or agent running tests outside Gradle — from the IDE, from a script that invokes JUnit directly, or from a future test runner — will see all 33 tests fail on TC-1 with no indication that the working directory is the root cause. Debugging requires knowing to check the Gradle build file for a workingDir override. As the spec count grows, the diagnosis cost grows proportionally."
    },
    {
      "id": "PAL-07",
      "title": "-Xcontext-parameters baked into both convention plugins — every module opts into an experimental flag",
      "severity": "low",
      "category": "coupling",
      "location": "buildSrc/src/main/kotlin/khaos.kotlin-kmp.gradle.kts:12, buildSrc/src/main/kotlin/khaos.kotlin-jvm.gradle.kts:12",
      "description": "-Xcontext-parameters is enabled globally in both convention plugins. The design justifies this because khaos-cmd is the load-bearing module for context parameters. However, khaos-memory, khaos-graph, khaos-shader, and khaos-test-harness have no stated need for context parameters now and may never have one. In Kotlin 2.3.x, context parameters remain a language preview — the flag name could be renamed or removed in a future stable release. If that happens, every module breaks simultaneously regardless of whether it uses the feature.",
      "evidence": "buildSrc/src/main/kotlin/khaos.kotlin-kmp.gradle.kts line 12: 'freeCompilerArgs.add(\"-Xcontext-parameters\")'. buildSrc/src/main/kotlin/khaos.kotlin-jvm.gradle.kts line 12: same. Design doc D1: '-Xcontext-parameters flag is confirmed available in 2.1+ and carries forward into 2.3.x'. Assumption A3: 'flag may graduate or be renamed in a 2.3.x release'.",
      "future_cost": "When context parameters graduate to stable or the flag is renamed in a Kotlin 2.x release, both convention plugins need updating. That is a single-line fix — but the broader cost is that a build flag change across all modules cannot be attributed to any module's actual language requirements. Adding a third convention plugin or a per-module opt-in becomes necessary if a module ever needs a different compiler flag profile."
    },
    {
      "id": "PAL-08",
      "title": "kotest-property declared in catalog but wired nowhere — catalog drift starts here",
      "severity": "low",
      "category": "maintainability",
      "location": "gradle/libs.versions.toml:19",
      "description": "libs.versions.toml declares kotest-property in [libraries]. Neither convention plugin includes it in testImplementation, and no build.gradle.kts references it. It was declared speculatively (the design lists it under D4 catalog structure). Unused catalog entries accumulate as the project grows and make it harder to determine what is actually in the dependency graph.",
      "evidence": "gradle/libs.versions.toml line 19: 'kotest-property = { module = \"io.kotest:kotest-property\", version.ref = \"kotest\" }'. grep for 'kotest-property' in all *.kts and *.kt files: zero matches in source files.",
      "future_cost": "Minor but compounding: each speculative catalog entry adds noise for the next agent reading the catalog to understand actual dependencies. If Gradle's catalog unused-entry lint is ever enabled, it flags this. If kotest-property's version is ever pinned separately (e.g. due to a version conflict), it gets updated without knowing nothing consumes it."
    },
    {
      "id": "PAL-09",
      "title": "Test package khaos.spike diverges from project domain dev.khaos.* — package archaeology starts now",
      "severity": "low",
      "category": "maintainability",
      "location": "khaos-test-harness/src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:1, SpikeShader1DecisionSpec.kt:1, SpikeShader2DecisionSpec.kt:1",
      "description": "Production code uses the package prefix dev.khaos (dev.khaos.test, dev.khaos.core, etc.). The spike test files use khaos.spike — no 'dev.' prefix, 'spike' as the namespace qualifier. This is a carry-over from when the tests lived in the root module before multi-module scaffolding. They were migrated in place without normalizing the package.",
      "evidence": "khaos-test-harness/src/main/kotlin/dev/khaos/test/KhaosTestHarness.kt: 'package dev.khaos.test'. khaos-test-harness/src/test/kotlin/khaos/spike/*.kt: 'package khaos.spike'.",
      "future_cost": "Low friction now. When khaos-test-harness acquires real test infrastructure (TEST-1 through TEST-6 per the design's module map), new test files will need to decide: follow khaos.spike or dev.khaos.test.spike or dev.khaos.test. Each decision made inconsistently makes future refactoring more expensive. IDE navigation requires knowing two roots. If a linter or import-order rule is added, it fires on both namespaces differently."
    },
    {
      "id": "PAL-10",
      "title": "Six _bmad/memory/agent-dev/ files bundled in a build-infrastructure PR — memory and code changes entangled",
      "severity": "low",
      "category": "maintainability",
      "location": "_bmad/memory/agent-dev/BOND.md, MEMORY.md, INDEX.md, PERSONA.md, CREED.md, CAPABILITIES.md, sessions/2026-04-19.md, sessions/2026-04-18.md",
      "description": "The PR commits six agent-dev memory files alongside the build scaffolding. BOND.md (first committed in a prior chore commit) has already drifted: it recorded 'Kotlin 2.1.20' and 'Source: src/test/kotlin/khaos/' — both stale facts that are directly contradicted by the project state after this PR. The current working copy shows _bmad/memory/agent-dev/BOND.md, MEMORY.md, and INDEX.md as modified (not yet committed), meaning the session's memory updates are already partially out of sync with HEAD. Mixing session memory commits with feature PR commits obscures what changed for build-infrastructure purposes and makes bisect and revert noisier.",
      "evidence": "git status shows ' M _bmad/memory/agent-dev/BOND.md' (working tree modified, not staged). BOND.md at commit 422007e contained 'Kotlin 2.1.20' and 'Source: src/test/kotlin/khaos/spike/'. Commit 1dfea3f (the main feature commit) does NOT include agent-dev memory files — they were committed in a prior chore commit and updated in the working tree now.",
      "future_cost": "When the build breaks and a developer bisects to find the regression, memory file changes create noise in the diff. When BOND.md claims the wrong Kotlin version, a future agent reading it will initialize with the wrong assumption and produce code or tests that fail silently. The 'stale version in docs' bug is already present — it will accumulate."
    }
  ]
}
```

---

## Summary by severity

| Severity | Count | IDs |
|---|---|---|
| High | 2 | PAL-01, PAL-03 |
| Medium | 3 | PAL-02, PAL-04, PAL-05 |
| Low | 5 | PAL-06, PAL-07, PAL-08, PAL-09, PAL-10 |

## Highest-cost items to address before Issue #4 begins

1. **PAL-01** — Update TC-02 in the test plan to reflect that LWJGL native classifiers cannot go in the TOML catalog and must be declared inline. Add a note on how they are expected to be declared in module build files.
2. **PAL-03** — Resolve the KSP version before the first module applies KSP. Either verify that 2.3.6 resolves (unlikely) or correct to the kotlinVersion-prefixed format.
3. **PAL-05** — Create a tracking note or follow-on issue for KMP JaCoCo wiring; 'deferred to Issue #4' is not actionable without an explicit ticket or convention plugin stub with a TODO comment explaining the wiring pattern needed.

## Items acceptable as-is for this hobby-scale project

- **PAL-04** (missing group ID): No publishing intent near-term; add in a dedicated publishing issue.
- **PAL-07** (global -Xcontext-parameters): Single-point change when/if the flag graduates; the risk is bounded.
- **PAL-08, PAL-09, PAL-10**: Low-friction, no immediate failure path.
