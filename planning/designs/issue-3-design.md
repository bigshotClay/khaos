# F-1: Gradle Multi-Module Project Scaffolding

**Issue:** #3
**Date:** 2026-04-19
**Status:** Decision ready
**Author:** Atlas (agent-architect)

---

## Context

The project currently has a single-module Gradle setup (`build.gradle.kts` + `settings.gradle.kts`, no `libs.versions.toml`, no modules). Issues #1 and #2 established that `buildSrc` will host custom Gradle tasks (`ShaderCompileTask`, `SpirvValidateTask`, `ShaderBindingGenTask`), but `buildSrc` itself does not exist yet. Issue #3 creates the structural foundation every subsequent kernel issue builds on.

**Current state:** `settings.gradle.kts` declares only `rootProject.name = "khaos"`. Root `build.gradle.kts` applies `kotlin("jvm") 2.1.20` and directly declares Kotest. The spike tests in `src/test/kotlin/khaos/spike/` have no module home.

---

## Module Responsibility Map

| Module | Issue area | Settled BOND source |
|---|---|---|
| `khaos-core` | VK-1–VK-8: `VulkanOutcome`, typed handles, instance, device, queues, swapchain, render pass, sync | Handles as `@JvmInline value class`; `VulkanOutcome` sealed hierarchy |
| `khaos-memory` | MEM-1–MEM-3: VMA allocator, `FrameIndex`, deferred deletion queue | VMA + typed allocations + scope-tracked lifetimes |
| `khaos-shader` | SHADER-1–SHADER-2: SPIR-V types, binding types, Gradle tasks | Gradle task chain: compileShaders → reflectShaders → generateBindings |
| `khaos-graph` | GRAPH-1–GRAPH-5: render graph data model, compiler, pipeline state, execution | Graph as data structure, compiler as pure function |
| `khaos-cmd` | VK-7 + GRAPH-4: `RecordingScope` context type, command pool | Functional core / imperative shell; `-Xcontext-parameters` load-bearing |
| `khaos-test-harness` | TEST-1–TEST-6: math property tests, record-phase framework, golden image harness | Test infrastructure; Kotest runner; Lavapipe headless |

### Inter-module dependency graph

```
khaos-core  ◄──────────────────────────────────────
     ▲                 ▲              ▲              |
khaos-memory    khaos-shader    khaos-graph    khaos-cmd
     ▲               ▲               ▲              ▲
     └───────────────┴───────────────┴──────────────┘
                 khaos-test-harness
                 (test scope only)
```

`khaos-graph` depends on both `khaos-core` and `khaos-shader` (compiled render passes reference shader binding types). `khaos-cmd` depends on `khaos-core`, `khaos-graph`, and `khaos-memory`.

---

## Design Decisions

### D1 — Kotlin Multiplatform vs. Kotlin JVM

**Settled in BOND:** "Kotlin Multiplatform (JVM target first, K/N later)." Not re-litigated here.

**Concrete implication for this issue:** All modules except `khaos-test-harness` apply the `kotlin-multiplatform` Gradle plugin with a single JVM target. Source directories are `src/commonMain/kotlin/` and `src/jvmMain/kotlin/` (not the flat `src/main/kotlin/`). Every implementation agent writing code for these modules must use `commonMain` for pure Kotlin types and `jvmMain` for LWJGL-dependent code.

`khaos-test-harness` applies `kotlin("jvm")` only — test frameworks (Kotest, Lavapipe integration) are JVM-forever.

**KMP jvmTest vs. jvmMain test source:**
KMP modules run tests in `src/jvmTest/kotlin/`. The Kotest JVM runner is declared in `jvmTest` dependencies. This is a departure from the current flat `src/test/kotlin/` and must be communicated to implementation agents.

**Assumption A1:** Clay confirms `khaos-test-harness` as JVM-only (not KMP). Flag if wrong.

---

### D2 — Build Logic Location: buildSrc vs. build-logic composite

Two options:

| Dimension | `buildSrc/` | `build-logic/` composite build |
|---|---|---|
| Convention plugins | Precompiled script plugins in `buildSrc/src/main/kotlin/` | Same, but in a separate included build |
| Version catalog access | Requires `settings.gradle.kts` in buildSrc that re-imports the toml | Shares the root catalog natively via `versionCatalogs` in settings |
| Agentic legibility | Single place — agents scan `buildSrc/` | Two places — root and `build-logic/`; slightly more navigation |
| Kotlin DSL plugin use | Fully supported | Fully supported |
| Rebuild on change | Auto-rebuilt when any file changes | Same |
| Ecosystem precedent | Industry default; all Gradle guides use this | Preferred in multi-repo / library publishing setups |

**Recommendation: `buildSrc/`** — simpler, agentic-legible, standard. The version catalog access gotcha (see below) is a one-line fix.

**Version catalog access from buildSrc:** Gradle does not expose the root version catalog to buildSrc automatically. Workaround: add `buildSrc/settings.gradle.kts` that reimports the catalog:

```kotlin
// buildSrc/settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") { from(files("../gradle/libs.versions.toml")) }
    }
}
```

This is explicit and traceable. Convention plugins can then reference `libs.versions.kotlin` etc.

---

### D3 — Convention Plugin Design

Two convention plugins in `buildSrc/src/main/kotlin/`:

**`kotlin-kmp.gradle.kts`** — applied to `khaos-core`, `khaos-memory`, `khaos-shader`, `khaos-graph`, `khaos-cmd`:

```kotlin
plugins {
    kotlin("multiplatform")
}

kotlin {
    jvmToolchain(21)
    jvm()
    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation(libs.kotest.runner.junit5)
            implementation(libs.kotest.assertions.core)
        }
    }
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
```

**`kotlin-jvm.gradle.kts`** — applied to `khaos-test-harness`:

```kotlin
plugins {
    kotlin("jvm")
    jacoco
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

dependencies {
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        xml.outputLocation = layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml")
    }
}
```

> **Approved deviation:** Design specified `finalizedBy(tasks.jacocoTestReport)`. Implementation uses `tasks.jacocoTestReport { dependsOn(tasks.test) }` instead. Reason: `finalizedBy` runs even when tests fail, producing JaCoCo noise on top of the actual failure (COR-05). The coverage gate explicitly runs `./gradlew test jacocoTestReport`, so auto-triggering via `finalizedBy` is unnecessary.

**Note on jacoco:** KMP's `jvmTest` task is wired differently — JaCoCo requires explicit wiring per compilation target. For KMP modules, defer JaCoCo integration to a later issue (or to the coverage tool used by shift-left-dev, which uses diff-cover). For Issue #3, only `khaos-test-harness` gets JaCoCo.

---

### D4 — Version Catalog Structure

Full `gradle/libs.versions.toml`:

```toml
[versions]
kotlin        = "2.3.20"
ksp           = "2.3.6"
kotest        = "5.9.1"
lwjgl         = "3.3.6"

[libraries]
# ── LWJGL ─────────────────────────────────────────────────────
lwjgl-bom             = { module = "org.lwjgl:lwjgl-bom",     version.ref = "lwjgl" }
lwjgl-core            = { module = "org.lwjgl:lwjgl" }
lwjgl-vulkan          = { module = "org.lwjgl:lwjgl-vulkan" }
lwjgl-shaderc         = { module = "org.lwjgl:lwjgl-shaderc" }  # declared; NOT used at build time (Spike 1)

# LWJGL core natives — runtime classifiers
lwjgl-natives-linux          = { module = "org.lwjgl:lwjgl",         classifier = "natives-linux" }
lwjgl-natives-linux-arm64    = { module = "org.lwjgl:lwjgl",         classifier = "natives-linux-arm64" }
lwjgl-natives-macos          = { module = "org.lwjgl:lwjgl",         classifier = "natives-macos" }
lwjgl-natives-macos-arm64    = { module = "org.lwjgl:lwjgl",         classifier = "natives-macos-arm64" }
lwjgl-natives-windows        = { module = "org.lwjgl:lwjgl",         classifier = "natives-windows" }

# LWJGL Vulkan natives — macOS only (MoltenVK layer); Linux/Windows use system ICD
lwjgl-vulkan-natives-macos       = { module = "org.lwjgl:lwjgl-vulkan", classifier = "natives-macos" }
lwjgl-vulkan-natives-macos-arm64 = { module = "org.lwjgl:lwjgl-vulkan", classifier = "natives-macos-arm64" }

# ── Kotest ────────────────────────────────────────────────────
kotest-runner-junit5    = { module = "io.kotest:kotest-runner-junit5",  version.ref = "kotest" }
kotest-assertions-core  = { module = "io.kotest:kotest-assertions-core", version.ref = "kotest" }
kotest-property         = { module = "io.kotest:kotest-property",        version.ref = "kotest" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-jvm           = { id = "org.jetbrains.kotlin.jvm",           version.ref = "kotlin" }
ksp                  = { id = "com.google.devtools.ksp",             version.ref = "ksp" }
```

**LWJGL Vulkan native note:** On Linux and Windows, Vulkan is loaded via the system's ICD loader — there is no bundled native JAR. On macOS, `lwjgl-vulkan` bundles MoltenVK headers and requires the `natives-macos` / `natives-macos-arm64` classifiers at runtime. Modules using `lwjgl-vulkan` on macOS must declare these classifiers as `runtimeOnly`.

**KSP version note:** KSP changed its versioning scheme at 2.3.x — it no longer embeds the Kotlin version in the version string. KSP `2.3.6` is the latest as of this design; verify compatibility with Kotlin 2.3.20 before implementation. If mismatch is found, the version field is the only change required.

**Kotlin version note:** The issue requires "2.2+". This design uses 2.3.20 (latest stable). The `-Xcontext-parameters` flag is confirmed available in 2.1+ and carries forward into 2.3.x.

---

### D5 — KSP Plugin Placement

KSP is declared in the version catalog and applied at root with `apply false`. Modules that need it apply it explicitly. As of Issue #3, no module requires KSP (Spike 2 ruled it out for shader binding codegen). KSP is declared available for future processors.

Root `build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm)           apply false
    alias(libs.plugins.ksp)                  apply false
}
```

> **Approved deviation:** `alias(libs.plugins.kotlin.multiplatform) apply false` and `alias(libs.plugins.kotlin.jvm) apply false` are intentionally absent. Adding them to root causes "already on classpath with unknown version" because `buildSrc/build.gradle.kts` puts `kotlin-gradle-plugin` on the main build classpath. Only `alias(libs.plugins.ksp) apply false` is declared at root (KSP is not on the buildSrc classpath). See `_bmad/memory/agent-dev/MEMORY.md` — "Kotlin plugin + buildSrc classpath conflict".

---

### D6 — LWJGL in Module Builds

LWJGL is consumed via BOM. Convention plugins do NOT declare LWJGL — each module declares its own LWJGL dependencies against the BOM. Pattern:

```kotlin
// khaos-core/build.gradle.kts
plugins { id("khaos.kotlin-kmp") }

kotlin {
    sourceSets {
        jvmMain.dependencies {
            implementation(platform(libs.lwjgl.bom))
            implementation(libs.lwjgl.core)
            implementation(libs.lwjgl.vulkan)
            runtimeOnly(libs.lwjgl.natives.linux)
            runtimeOnly(libs.lwjgl.natives.linux.arm64)
            runtimeOnly(libs.lwjgl.natives.macos)
            runtimeOnly(libs.lwjgl.natives.macos.arm64)
            runtimeOnly(libs.lwjgl.natives.windows)
            runtimeOnly(libs.lwjgl.vulkan.natives.macos)
            runtimeOnly(libs.lwjgl.vulkan.natives.macos.arm64)
        }
    }
}
```

Not all modules need all LWJGL artifacts. Issue #3 only creates placeholders — LWJGL dependencies are wired in the issues that implement each module.

---

### D7 — Root Project and Existing Spike Tests

After multi-module introduction, the root project becomes a pure coordination project with no source code. The existing spike tests in `src/test/kotlin/khaos/spike/` must be addressed.

**Options:**

| Option | Tradeoff |
|---|---|
| **A: Migrate to `khaos-test-harness`** | Clean; spike decision tests serve as architectural compliance tests |
| **B: Migrate to `khaos-shader`** | Tests are shader-specific; keeps them adjacent to SHADER-1/2 implementation |
| **C: Delete** | Spike decisions are committed; tests no longer serve a purpose |
| **D: Keep in root as JVM project** | Root retains `kotlin("jvm")` and spike tests; root builds independently of modules |

**Decision: Option A — migrate to `khaos-test-harness`** (confirmed 2026-04-19). Spike decision tests validate build toolchain architecture and belong in the test harness. Migration is in scope for Issue #3.

---

### D8 — `settings.gradle.kts` Structure

```kotlin
// settings.gradle.kts
rootProject.name = "khaos"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

include(
    ":khaos-core",
    ":khaos-memory",
    ":khaos-shader",
    ":khaos-graph",
    ":khaos-cmd",
    ":khaos-test-harness",
)
```

`dependencyResolutionManagement` with `FAIL_ON_PROJECT_REPOS` prevents individual modules from declaring their own `repositories {}` blocks — all resolution is centralized in settings. This is the modern Gradle convention.

---

## File Structure Blueprint

```
khaos/
├── gradle/
│   └── libs.versions.toml                  # NEW — version catalog
├── settings.gradle.kts                     # REPLACE — add modules + dependencyResolutionManagement
├── build.gradle.kts                        # REPLACE — root coordination only (plugin declarations)
├── buildSrc/
│   ├── settings.gradle.kts                 # NEW — reimport libs.versions.toml for catalog access
│   ├── build.gradle.kts                    # NEW — kotlin-dsl plugin + catalog
│   └── src/main/kotlin/
│       ├── kotlin-kmp.gradle.kts           # NEW — KMP convention plugin
│       └── kotlin-jvm.gradle.kts           # NEW — JVM convention plugin
├── khaos-core/
│   ├── build.gradle.kts                    # NEW — applies khaos.kotlin-kmp
│   └── src/commonMain/kotlin/dev/khaos/core/
│       └── KhaosCore.kt                    # placeholder
├── khaos-memory/
│   ├── build.gradle.kts                    # NEW — applies khaos.kotlin-kmp
│   └── src/commonMain/kotlin/dev/khaos/memory/
│       └── KhaosMemory.kt                  # placeholder
├── khaos-shader/
│   ├── build.gradle.kts                    # NEW — applies khaos.kotlin-kmp
│   └── src/commonMain/kotlin/dev/khaos/shader/
│       └── KhaosShader.kt                  # placeholder
├── khaos-graph/
│   ├── build.gradle.kts                    # NEW — applies khaos.kotlin-kmp
│   └── src/commonMain/kotlin/dev/khaos/graph/
│       └── KhaosGraph.kt                   # placeholder
├── khaos-cmd/
│   ├── build.gradle.kts                    # NEW — applies khaos.kotlin-kmp
│   └── src/commonMain/kotlin/dev/khaos/cmd/
│       └── KhaosCmd.kt                     # placeholder
└── khaos-test-harness/
    ├── build.gradle.kts                    # NEW — applies khaos.kotlin-jvm
    └── src/main/kotlin/dev/khaos/test/
        └── KhaosTestHarness.kt             # placeholder
```

---

## Assumptions to Validate

| # | Assumption | Risk if wrong |
|---|---|---|
| A1 | `khaos-test-harness` is JVM-only (Kotest runner, Lavapipe integration) | Low — easy to switch to KMP Kotest if needed |
| A2 | KSP 2.3.6 is compatible with Kotlin 2.3.20 | Medium — version number mismatch; check KSP release notes before implementation |
| A3 | `-Xcontext-parameters` flag name is stable in Kotlin 2.3.20 | Low — confirmed for 2.1.x; flag may graduate or be renamed in a 2.3.x release |
| A4 | Kotlin 2.3.20 is stable and available on Gradle Plugin Portal | Low — confirmed on Maven Central |
| A5 | Spike tests migrated to `khaos-test-harness` | Resolved — confirmed Option A |
| A6 | JaCoCo integration for KMP modules deferred to later | Medium — shift-left-dev coverage gate needs JaCoCo or diff-cover; verify coverage strategy for KMP target |

---

## Blocking Considerations

**None identified that would prevent scaffolding.** However:

- **A2 (KSP version):** If KSP 2.3.6 does not work with Kotlin 2.3.20, downgrade Kotlin to 2.2.21 (`ksp = "2.2.21-2.0.5"` is confirmed for that version). The `-Xcontext-parameters` flag is available in both.

- **A6 (JaCoCo / KMP coverage):** The shift-left-dev coverage gate runs diff-cover against a Cobertura XML. KMP's `jvmTest` task requires explicit JaCoCo wiring different from `kotlin("jvm")`. The `kotlin-kmp.gradle.kts` convention plugin should be extended with JaCoCo wiring before any KMP module goes through the coverage gate. Recommend flagging this as a follow-on task when Issue #4 (first KMP module implementation) approaches review.

---

## Sources

| Claim | Source |
|---|---|
| Kotlin 2.3.20 stable | GitHub Releases: JetBrains/kotlin |
| KSP 2.3.6 latest | Maven Central: com.google.devtools.ksp |
| LWJGL 3.3.6 latest | Maven Central: org.lwjgl:lwjgl-bom |
| LWJGL Vulkan native macOS requirement | LWJGL platform guide; `lwjgl-vulkan` has no Linux/Windows native JAR |
| KSP versioning scheme change at 2.3.x | Maven Central version history |
| buildSrc catalog access pattern | Gradle docs: Sharing dependency versions |
| `FAIL_ON_PROJECT_REPOS` convention | Gradle docs: Centralizing repositories declaration |
| `-Xcontext-parameters` available in 2.1+ | Kotlin 2.1 changelog; confirmed in 2.2.x |
