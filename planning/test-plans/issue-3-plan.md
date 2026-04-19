# Test Plan — Issue #3: F-1 Gradle Multi-Module Project Scaffolding

**Issue:** #3
**Design source:** `planning/designs/issue-3-design.md`
**Plan date:** 2026-04-19
**Author:** Sentinel

---

## Scope

This plan covers the Gradle multi-module scaffolding. All test cases are verified by file inspection, `./gradlew` task execution, or Kotest specs running in `khaos-test-harness`. There is no application logic — tests validate structure, build hygiene, and spike test migration.

---

## Test Cases

### Acceptance

**TC-01 — All six modules included in settings**
- `settings.gradle.kts` contains `include(`:khaos-core`, `:khaos-memory`, `:khaos-shader`, `:khaos-graph`, `:khaos-cmd`, `:khaos-test-harness`)` (all six, any order)
- No additional modules are included
- Verification: static file check — `settings.gradle.kts` contains each `:khaos-*` identifier

---

**TC-02 — LWJGL declared in version catalog with core entries; native strategy documented**

Classifier dispute resolution: Gradle 9.x TOML does NOT support the `classifier` key in `[libraries]` entries. This is confirmed by `_bmad/memory/agent-dev/MEMORY.md` ("TOML `classifier` key: NOT supported in `[libraries]` entries — LWJGL native variants cannot go in the catalog"), which reflects a first-hand build failure. The design doc D4 specified classifier entries but the implementation correctly removed them after encountering this constraint. The 7 classifier-based assertions from the original TC-02 are removed; the inline declaration strategy is the approved approach.

- `gradle/libs.versions.toml` exists
- `[versions]` block contains `lwjgl = "3.3.6"` (or compatible)
- `[libraries]` block contains all four non-native LWJGL entries:
  - `lwjgl-bom` with `version.ref = "lwjgl"`
  - `lwjgl-core` (`org.lwjgl:lwjgl`)
  - `lwjgl-vulkan` (`org.lwjgl:lwjgl-vulkan`)
  - `lwjgl-shaderc` (`org.lwjgl:lwjgl-shaderc`)
- `[libraries]` block does NOT contain `classifier =` entries (Gradle 9.x TOML limitation)
- `libs.versions.toml` contains a comment explaining that native classifier variants (e.g., `natives-linux`, `natives-macos-arm64`) must be declared inline at the module level using `variantOf()` or `"org.lwjgl:lwjgl::natives-linux"` notation — not as catalog entries
- Verification: static file check on each entry; grep confirms absence of `classifier =` and presence of inline-strategy comment

---

**TC-03 — `-Xcontext-parameters` present in KMP convention plugin**
- `buildSrc/src/main/kotlin/khaos.kotlin-kmp.gradle.kts` contains `"-Xcontext-parameters"` in a `freeCompilerArgs` block
- Verification: static file check

---

**TC-04 — `-Xcontext-parameters` present in JVM convention plugin**
- `buildSrc/src/main/kotlin/khaos.kotlin-jvm.gradle.kts` contains `"-Xcontext-parameters"` in a `freeCompilerArgs` block
- Verification: static file check

---

**TC-05 — KSP declared at root with `apply false`**
- Root `build.gradle.kts` contains `alias(libs.plugins.ksp) apply false`
- `gradle/libs.versions.toml` `[plugins]` block contains a `ksp` entry
- No submodule `build.gradle.kts` applies KSP (all are placeholders only in this issue)
- Verification: static file check

---

**TC-06 — Kotest declared in version catalog**
- `gradle/libs.versions.toml` contains `kotest` in `[versions]`
- `[libraries]` block contains all three entries: `kotest-runner-junit5`, `kotest-assertions-core`, `kotest-property`
- Verification: static file check

---

**TC-07 — Clean build from fresh checkout**
- `./gradlew build --no-daemon` exits 0
- Build output contains no Kotlin compiler `warning:` or `w:` lines
- Verification: execute task; scan output for warning markers

Implementation note: Run with `--warning-mode all` to surface suppressed warnings. A warning-free scaffold is the acceptance bar.

---

**TC-08 — Each module has exactly one placeholder source file**

| Module | Expected file path |
|---|---|
| `khaos-core` | `khaos-core/src/commonMain/kotlin/dev/khaos/core/KhaosCore.kt` |
| `khaos-memory` | `khaos-memory/src/commonMain/kotlin/dev/khaos/memory/KhaosMemory.kt` |
| `khaos-shader` | `khaos-shader/src/commonMain/kotlin/dev/khaos/shader/KhaosShader.kt` |
| `khaos-graph` | `khaos-graph/src/commonMain/kotlin/dev/khaos/graph/KhaosGraph.kt` |
| `khaos-cmd` | `khaos-cmd/src/commonMain/kotlin/dev/khaos/cmd/KhaosCmd.kt` |
| `khaos-test-harness` | `khaos-test-harness/src/main/kotlin/dev/khaos/test/KhaosTestHarness.kt` |

- Each file exists
- Each file declares `package dev.khaos.<module>` matching the path
- Verification: file existence + static content check

---

### Design Contract

**TC-09 — KMP vs JVM plugin split (D1)**
- Each KMP module (`khaos-core`, `khaos-memory`, `khaos-shader`, `khaos-graph`, `khaos-cmd`): `build.gradle.kts` applies `id("khaos.kotlin-kmp")` and does NOT declare `kotlin("jvm")` directly
- `khaos-test-harness/build.gradle.kts` applies `id("khaos.kotlin-jvm")` and does NOT declare `kotlin("multiplatform")` directly
- Verification: static file check per module

---

**TC-10 — KMP source directory layout (D1)**
- Each KMP module contains `src/commonMain/kotlin/` as a source root (file tree confirms directory exists)
- `khaos-test-harness` does NOT contain `src/commonMain/` — its source root is `src/main/kotlin/`
- Kotest dependencies in KMP modules land in `jvmTest` — convention plugin declares them under `jvmTest.dependencies`, not `commonTest`
- Verification: static file check on directory structure and convention plugin DSL

---

**TC-11 — `buildSrc` structure and catalog access (D2)**
- `buildSrc/settings.gradle.kts` exists
- Contains `from(files("../gradle/libs.versions.toml"))` inside a `versionCatalogs { create("libs") { ... } }` block
- `buildSrc/build.gradle.kts` exists and declares the `kotlin-dsl` plugin
- Verification: static file check

---

**TC-12 — Both convention plugins compile (D3)**
- `buildSrc/src/main/kotlin/khaos.kotlin-kmp.gradle.kts` exists
- `buildSrc/src/main/kotlin/khaos.kotlin-jvm.gradle.kts` exists
- Plugin ID applied in KMP submodule builds is `id("khaos.kotlin-kmp")`
- Plugin ID applied in `khaos-test-harness` build is `id("khaos.kotlin-jvm")`
- `./gradlew :buildSrc:assemble` (or equivalent) exits 0
- Verification: file existence + static plugin ID check + Gradle task execution

Implementation note: The `khaos.` prefix in the file name is mandatory — it is how Gradle derives the plugin ID (`khaos.kotlin-kmp.gradle.kts` → plugin ID `khaos.kotlin-kmp`). Files named `kotlin-kmp.gradle.kts` (without prefix) would produce a different plugin ID and would not match the `id("khaos.kotlin-kmp")` calls in module build files. If `buildSrc` does not expose a standalone `:buildSrc:assemble` target, the full `./gradlew build` from TC-07 covers this.

---

**TC-13 — Pinned versions in catalog (D4)**
- `gradle/libs.versions.toml` `[versions]` block contains:
  - `kotlin = "2.3.20"` (or higher 2.x; must be 2.2+)
  - `ksp` entry (any value; A2 assumption — compatible with Kotlin version in use)
  - `kotest = "5.9.1"` (or higher 5.x)
  - `lwjgl = "3.3.6"` (or higher 3.x)
- Verification: static file check

---

**TC-14 — JVM toolchain 21 in both convention plugins (D3)**
- `buildSrc/src/main/kotlin/khaos.kotlin-kmp.gradle.kts` contains `jvmToolchain(21)`
- `buildSrc/src/main/kotlin/khaos.kotlin-jvm.gradle.kts` contains `jvmToolchain(21)`
- Verification: static file check

---

**TC-15 — `FAIL_ON_PROJECT_REPOS` enforced in settings (D8)**
- `settings.gradle.kts` contains `RepositoriesMode.FAIL_ON_PROJECT_REPOS` inside a `dependencyResolutionManagement` block
- Verification: static file check

---

**TC-16 — Root is coordination-only (D8)**
- Root `build.gradle.kts` does NOT apply any language plugin with effect (no `apply plugin`, no `kotlin("jvm")` without `apply false`)
- All plugin declarations in root use `apply false`
- Root `build.gradle.kts` contains no `dependencies {}` block
- `alias(libs.plugins.ksp) apply false` is present in root `build.gradle.kts`
- [REQUIRES RESOLUTION] `alias(libs.plugins.kotlin.multiplatform) apply false` is present in root `build.gradle.kts` — D5 requires this, but `_bmad/memory/agent-dev/MEMORY.md` documents that re-declaring `kotlin-gradle-plugin` at root after buildSrc puts it on the classpath causes "already on classpath with unknown version" failure. If this cannot be declared at root due to the classpath conflict, document the deviation in the design as an approved exception to D5 and annotate this assertion accordingly.
- [REQUIRES RESOLUTION] `alias(libs.plugins.kotlin.jvm) apply false` is present in root `build.gradle.kts` — same classpath conflict constraint applies as above.
- Verification: static file check; classpath conflict resolution determines whether the two REQUIRES RESOLUTION assertions are enforced or documented as approved deviations

---

**TC-17 — Spike tests migrated out of root (D7)**
- `khaos-test-harness/src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt` exists
- `khaos-test-harness/src/test/kotlin/khaos/spike/SpikeShader1DecisionSpec.kt` exists
- `khaos-test-harness/src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt` exists
- `src/test/kotlin/khaos/spike/` does NOT exist in the root module (directory absent or empty)
- Verification: file existence check (new location) + absence check (old location)

---

**TC-18 — Migrated spike tests pass in new home (D7)**
- `./gradlew :khaos-test-harness:test` exits 0
- All three spike decision specs (`SpikeShader1DecisionSpec`, `SpikeShader2DecisionSpec`) pass
- Verification: Gradle task execution

Implementation note: The `SpikeDecisionDocument` helper constructs paths to `planning/decisions/` docs. After migration, those path references must resolve relative to the project root, not the submodule directory. The git `log --oneline` call in `committedToGit()` must also use the project root as working directory — verify the path base passed to `ProcessBuilder.directory()` is still correct after moving the test file.

---

### Gauntlet Shadow Findings (Loop 1)

The following TCs were added from the Gauntlet review shadow patch (`planning/shadows/issue-3-2026-04-19.md`). They address medium+ findings from the Loop 1 review.

---

**TC-19 — Root `build.gradle.kts` declares all required plugin aliases with `apply false` (D5)**
- [REQUIRES RESOLUTION] Root `build.gradle.kts` contains `alias(libs.plugins.kotlin.multiplatform) apply false`
- [REQUIRES RESOLUTION] Root `build.gradle.kts` contains `alias(libs.plugins.kotlin.jvm) apply false`
- Root `build.gradle.kts` contains `alias(libs.plugins.ksp) apply false`

Resolution note: D5 mandates all three declarations. MEMORY.md documents that re-declaring `kotlin-gradle-plugin` at root after buildSrc puts it on the classpath causes "already on classpath with unknown version" failure. If the two Kotlin plugin aliases cannot be declared at root due to the classpath conflict, this TC must be annotated as a known deviation from D5: document the buildSrc classpath constraint as an approved exception in the design doc and update this assertion to verify the deviation is documented rather than failing the implementation.

- Verification: static file content check

---

**TC-20 — `committedToGit()` git ProcessBuilder sanitizes child process environment**
- `SpikeDecisionDocument.committedToGit()` calls `process.environment().also { it.clear() }` (or equivalent) before any environment variable assignments on the git ProcessBuilder
- The git subprocess must not inherit GH_TOKEN, GITHUB_TOKEN, SSH_AUTH_SOCK, or other secrets from the parent JVM environment
- Rationale: GH subprocess in spike specs correctly clears env; git subprocess must match for consistency and to prevent credential leakage to git credential helpers or post-checkout hooks
- Verification: static code inspection — grep for `environment()` and `clear()` in `SpikeDecisionDocument.kt`

---

**TC-21 — Gradle wrapper distribution pinned with SHA-256 checksum**
- `gradle/wrapper/gradle-wrapper.properties` contains a `distributionSha256Sum=<64-char hex>` line
- The SHA-256 value matches the official Gradle distribution checksum from https://gradle.org/release-checksums/ for the declared version
- Rationale: without a pinned checksum, a MITM or CDN compromise can substitute an arbitrary Gradle distribution that executes arbitrary code on every developer machine and CI runner
- Verification: static file content check (64-char hex pattern); optional online checksum cross-reference

---

**TC-22 — `committedToGit()` timeout is not dead code**
- `SpikeDecisionDocument.committedToGit()` reads process stdout on a background thread (e.g., `CompletableFuture.supplyAsync`, coroutine, or explicit `Thread`) so that `waitFor(30, TimeUnit.SECONDS)` fires while the read is in progress
- The following pattern is unacceptable: `readText()` called on the same thread before `waitFor()` (makes the timeout unreachable while the process is alive; git hang → infinite test stall)
- Acceptable patterns: `CompletableFuture.supplyAsync { inputStream.bufferedReader().readText() }` mirroring the pattern already used in the spike specs for the gh subprocess
- Verification: static code inspection — confirm async read wrapper present before `waitFor()` in `committedToGit()`

---

**TC-23 — Convention plugins guard `Optional` catalog access with named error**
- `khaos.kotlin-kmp.gradle.kts` does NOT call `findLibrary(...).get()` without a present guard
- `khaos.kotlin-jvm.gradle.kts` does NOT call `findLibrary(...).get()` without a present guard
- Acceptable patterns:
  - `findLibrary("kotest-runner-junit5").orElseThrow { GradleException("Catalog entry 'kotest-runner-junit5' not found") }`
  - `findLibrary("...").orNull() ?: error("Catalog entry '...' missing")`
- Unacceptable: bare `.get()` on `Optional` — throws `NoSuchElementException` at Gradle configuration time across all modules with no actionable context
- Verification: static code inspection of both convention plugin files

---

**TC-24 — KSP version resolves as a published artifact**
- The `ksp` version string in `gradle/libs.versions.toml` resolves to a published artifact on Maven Central or the Gradle Plugin Portal
- Acceptable verification methods:
  - (a) Confirm artifact exists at `search.maven.org/artifact/com.google.devtools.ksp/symbol-processing-gradle-plugin/{version}`
  - (b) Run `./gradlew dependencyInsight --dependency com.google.devtools.ksp --configuration classpath` and assert no "not found" error
- Note: KSP versions for Kotlin 2.x historically follow the `<kotlinVersion>-<kspRelease>` format (e.g., `2.0.21-1.0.25`). The design doc D4 claims the versioning scheme changed at 2.3.x; verify this is true before accepting a standalone version string like `2.3.6`. If `2.3.6` does not exist on any registry, correct to the appropriate format per KSP release notes.
- Verification: external resolution check — guard with env-var presence or skip in offline mode; static version format check

---

**TC-25 — `khaos-test-harness` source root path computed robustly after migration**
- The spike spec files resolve the project root using `System.getProperty("user.dir")` or an equivalent mechanism set by the Gradle test task's `workingDir` property
- `khaos-test-harness/build.gradle.kts` sets `tasks.test { workingDir = rootProject.projectDir }` (or equivalent) so that `user.dir` resolves to the project root, not the submodule directory
- Rationale: after migration, the `SpikeDecisionDocument` path resolution must still reach `planning/decisions/` docs relative to the project root
- Verification: static check on `khaos-test-harness/build.gradle.kts` test task configuration + static check on path resolution in spec files

---

**TC-26 — `group = "dev.khaos"` declared for all modules**
- Root `build.gradle.kts` or `settings.gradle.kts` declares `group = "dev.khaos"` (via `allprojects` or equivalent), OR all six submodule `build.gradle.kts` files individually declare `group = "dev.khaos"`
- AC requirement: "Group ID: dev.khaos; artifact IDs match module names"
- Gradle defaults group to `""` — the AC requirement is silently dropped if this is not set
- Verification: static file content check across root and/or submodule build files

---

**TC-27 — KMP JaCoCo deferral is documented and tracked**
- `khaos.kotlin-kmp.gradle.kts` contains a TODO comment referencing the JaCoCo wiring deferral and the issue that will address it (acceptable: `// TODO(issue-N): wire JaCoCo for jvmTest task in KMP modules`)
- Design doc assumption A6 references a specific issue number for JaCoCo KMP wiring, not just "deferred to Issue #4" without a concrete reference
- Rationale: untracked technical debt — the shift-left-dev coverage gate will fail silently for all KMP modules when they receive implementation; a tracked TODO prevents this from being discovered at that time
- Verification: static code inspection of `khaos.kotlin-kmp.gradle.kts` + design doc A6 check

---

**TC-28 — LWJGL native declaration strategy demonstrated or documented**
- Since classifier entries are not in the version catalog (Gradle 9.x TOML limitation — confirmed), the native declaration strategy must be communicated to consumers via one of:
  - (a) A comment in `libs.versions.toml` explaining the inline `variantOf()` or `"org.lwjgl:lwjgl::natives-linux"` notation approach
  - (b) At least one module `build.gradle.kts` demonstrating the notation as a reference implementation (even in a comment)
- The AC requires natives for Linux x64/ARM64, macOS x64/Apple Silicon, and Windows x64. With classifier entries absent from the catalog, consumers must know how to declare them — this TC verifies the strategy is communicated, not just that it exists in MEMORY.md
- Verification: static file check on `libs.versions.toml` comment and/or module build files

---

**TC-29 — `committedToGit()` handles edge-case paths without throwing**
- `SpikeDecisionDocument(Path.of("bare-file.md")).committedToGit()` returns `false` and does not throw `NullPointerException` (bare filenames have `path.parent == null`)
- `SpikeDecisionDocument(Path.of("/nonexistent/dir/file.md")).committedToGit()` returns `false` and does not throw `IOException` (non-existent parent directory causes `ProcessBuilder.start()` to throw)
- The method's boolean contract must hold for all inputs, not only well-formed absolute paths
- Verification: Kotest unit test in `SpikeDecisionDocument` spec (or dedicated edge-case spec in `khaos-test-harness`)

---

**TC-30 — `jacocoTestReport` does not run when test task produces no `.exec` file**
- `khaos.kotlin-jvm.gradle.kts` wires `jacocoTestReport` such that it only runs when the test task produces coverage data (i.e., does not run unconditionally on test failure)
- Acceptable patterns: `jacocoTestReport { onlyIf { tasks.test.get().state.failure == null } }` or equivalent conditional wiring
- Unacceptable: bare `finalizedBy(tasks.jacocoTestReport)` with no guard — `finalizedBy` fires even when the test task fails before producing an `.exec` file, generating spurious JaCoCo noise on top of the actual failure
- Verification: static code inspection of `khaos.kotlin-jvm.gradle.kts` test task configuration

---

### Failure Paths

**TC-31 — `FAIL_ON_PROJECT_REPOS` rejects module-declared repositories**
- Test procedure (destructive, must revert):
  1. Add `repositories { mavenCentral() }` to any one module's `build.gradle.kts`
  2. Run `./gradlew build --no-daemon`
  3. Assert: exit code is non-zero
  4. Assert: build output contains `Build was configured to prefer settings repositories` or `RepositoriesMode.FAIL_ON_PROJECT_REPOS` (Gradle's error message for this violation)
  5. Revert the injected block
- Verification: destructive integration test

Implementation note: This is the only destructive TC. Keep it isolated — inject and revert within the same test execution script or document the revert step explicitly. Do not commit the broken state.

---

**TC-32 — Missing catalog access breaks buildSrc build**
- Rationale: verifies that `buildSrc/settings.gradle.kts` is not optional — removing it should break the `libs` accessor in convention plugins
- Test procedure (non-destructive — verify by inspection only):
  - `buildSrc/settings.gradle.kts` must contain the `versionCatalogs` block (TC-11 covers this)
  - Implementation note: Do NOT perform the destructive version (renaming the file). TC-11's static check is the sufficient gate — if the file exists and contains the catalog import, the enforced coupling exists.
- Verification: covered by TC-11

---

## Implementation Notes

**Root module removal:** After adding submodules, the root `build.gradle.kts` must NOT apply `kotlin("jvm")` or any source compilation plugin with effect. The existing root `build.gradle.kts` currently applies `kotlin("jvm")` directly — that must be replaced.

**Spike test path references:** `SpikeDecisionDocument` is constructed with a `Path`. After migration to `khaos-test-harness`, the specs must compute the root project directory correctly (e.g., from a system property set in the Gradle test task configuration, or from `System.getProperty("user.dir")`). Verify that `ProcessBuilder.directory()` in `committedToGit()` receives the project root, not the test harness module directory.

**JaCoCo on KMP deferred (A6):** The `khaos.kotlin-kmp.gradle.kts` convention plugin does not include JaCoCo wiring. The shift-left-dev coverage gate uses diff-cover against Cobertura XML — this will fail for KMP modules. Deferred to the issue that first implements a KMP module. Flag this as a follow-on task at that time and ensure TC-27 is satisfied (tracked TODO in the plugin file).

**KSP compatibility check (A2):** Before running `./gradlew build`, verify `ksp = "2.3.6"` resolves on Gradle Plugin Portal against Kotlin 2.3.20. If incompatible, change only the `ksp` version in `libs.versions.toml` — no other change required.
