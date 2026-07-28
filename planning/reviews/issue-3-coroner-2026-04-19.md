# Coroner Report — Issue #3: Gradle Multi-Module Project Scaffolding

**Reviewer:** The Coroner (failure-path analysis)
**Date:** 2026-04-19
**Issue:** #3 — F-1: Gradle multi-module project scaffolding
**Branch:** feature/issue-2-spike-shader-ksp-validation

---

```json
{
  "reviewer": "coroner",
  "findings": [
    {
      "id": "COR-01",
      "title": "Timeout dead letter: readText() before waitFor() makes the 30-second kill unreachable",
      "severity": "high",
      "category": "concurrency",
      "location": "SpikeDecisionDocument.kt:22-25",
      "description": "The 30-second timeout on the git subprocess is effectively dead code. readText() blocks the calling thread until the subprocess's stdout reaches EOF — which only happens after the process exits. waitFor(30, TimeUnit.SECONDS) is never reached until the process has already finished. If git hangs (e.g., waiting for a credential helper, a FUSE-mounted path, or a network-backed repository), readText() blocks indefinitely. destroyForcibly() is never called and the test thread stalls forever, hanging the entire test suite.",
      "evidence": "val output = result.inputStream.bufferedReader().readText()  // blocks until process exits\nval exited = result.waitFor(30, TimeUnit.SECONDS)            // only reached after exit\nif (!exited) { result.destroyForcibly(); return false }      // unreachable if git hangs",
      "scenario": "git encounters a slow path — credential prompt, FUSE mount, socket-backed worktree, or NFS — and never produces EOF. readText() blocks forever. The timeout guard is never evaluated."
    },
    {
      "id": "COR-02",
      "title": "IOException propagates uncaught when path.parent directory does not exist",
      "severity": "medium",
      "category": "exception",
      "location": "SpikeDecisionDocument.kt:19-21",
      "description": "ProcessBuilder.directory() accepts a File and start() throws IOException if that directory does not exist. path.parent.toFile() is the parent directory of the decision document path. If a caller constructs a SpikeDecisionDocument with a path whose parent directory has not yet been created (e.g., _bmad-output/planning-artifacts/designs/ is absent), committedToGit() throws IOException rather than returning false. The method contract implies a boolean return, but the IOException breaks this contract silently.",
      "evidence": "ProcessBuilder(\"git\", \"log\", \"--oneline\", \"--\", path.toString())\n    .directory(path.parent.toFile())  // throws IOException if directory doesn't exist\n    .start()",
      "scenario": "Tests are run before the _bmad-output directory tree has been created (e.g., fresh checkout, CI runner that only clones source, not generated artifacts). path.parent.toFile() points to a non-existent directory. ProcessBuilder.start() throws IOException. The test fails with an unexpected exception instead of a controlled false return."
    },
    {
      "id": "COR-03",
      "title": "LazyThreadSafetyMode.NONE on shared file-level val: data race on concurrent spec execution",
      "severity": "medium",
      "category": "concurrency",
      "location": "SpikeDecisionDocument.kt:12-15 / SpikeShader1DecisionSpec.kt:13 / SpikeShader2DecisionSpec.kt:13",
      "description": "The text property uses LazyThreadSafetyMode.NONE, which provides no synchronization. SpikeDecisionDocument instances are declared as file-level private val in each spec file. Kotest allows parallel spec execution via withGlobalCoroutineDispatcher or explicit concurrency config. If two test coroutines from different tests in the SAME spec run concurrently (e.g., via testCoroutineDispatcher or future Kotest versions enabling concurrent ShouldSpec tests), both threads can simultaneously observe _value === UNINITIALIZED_VALUE and both invoke initializer(), creating a race on the _value write. The winner's value is used; the loser's result is discarded. While Kotest's current default is sequential per-spec, this is a load-bearing assumption with no enforcement.",
      "evidence": "val text: String by lazy(LazyThreadSafetyMode.NONE) { ... }  // no synchronization\nprivate val decisionDoc = SpikeDecisionDocument(...)           // shared across all tests in spec",
      "scenario": "A future Kotest upgrade enables coroutine-level concurrency within ShouldSpec by default (as Kotest has discussed). Two tests in SpikeShader1DecisionSpec both access decisionDoc.text. Both observe UNINITIALIZED_VALUE, both call path.readText(), both write _value. One write is lost. Under JVM memory model without volatile/synchronized, the non-winning write may not be visible to subsequent tests at all."
    },
    {
      "id": "COR-04",
      "title": "catalogLibs.findLibrary(...).get() throws NoSuchElementException at configuration time on catalog key mismatch",
      "severity": "high",
      "category": "null",
      "location": "khaos.kotlin-kmp.gradle.kts:18-19 / khaos.kotlin-jvm.gradle.kts:16-17",
      "description": "findLibrary() returns Optional<Provider<MinimalExternalModuleDependency>>. Calling .get() on an empty Optional throws java.util.NoSuchElementException at Gradle configuration time (not at dependency resolution time). The convention plugin is applied to every KMP/JVM module, so a single catalog key rename or typo in libs.versions.toml causes the entire build to fail with a stack trace pointing inside the convention plugin, not at the changed catalog entry. The error message 'No value present' provides no indication of which key is missing.",
      "evidence": "implementation(catalogLibs.findLibrary(\"kotest-runner-junit5\").get())\nimplementation(catalogLibs.findLibrary(\"kotest-assertions-core\").get())",
      "scenario": "A future PR renames kotest-runner-junit5 to kotest-junit5-runner in libs.versions.toml (matching the Maven artifact ID). Every module applying khaos.kotlin-kmp or khaos.kotlin-jvm throws NoSuchElementException at configuration time. 'No value present' points into Gradle internals; the developer must trace back to the convention plugin to find the mismatched key name."
    },
    {
      "id": "COR-05",
      "title": "jacocoTestReport runs after failed test task and fails on missing .exec file",
      "severity": "medium",
      "category": "exception",
      "location": "khaos.kotlin-jvm.gradle.kts:22-23",
      "description": "tasks.test { finalizedBy(tasks.jacocoTestReport) } causes jacocoTestReport to execute even when the test task fails (finalizedBy semantics guarantee execution regardless of upstream outcome). If test fails before producing a .exec file (e.g., class loading error, test framework initialization failure), jacocoTestReport has no input data. Gradle's JaCoCo plugin will either silently produce an empty report or emit its own error about missing exec data. In both cases, the build output contains spurious JaCoCo error noise on top of the actual test failure, obscuring the root cause.",
      "evidence": "tasks.test {\n    useJUnitPlatform()\n    finalizedBy(tasks.jacocoTestReport)  // runs even when test fails\n}\ntasks.jacocoTestReport {\n    dependsOn(tasks.test)  // but jacocoTestReport has no exec data if test aborted early\n}",
      "scenario": "A Kotest class fails to initialize (e.g., missing dependency on classpath, IllegalStateException in companion object). The test task exits non-zero without writing a .exec file. jacocoTestReport runs, finds no exec data, and either silently produces empty XML or throws 'No coverage data found' error. The engineer sees JaCoCo noise first and must scroll up to find the original test failure."
    },
    {
      "id": "COR-06",
      "title": "KSP version '2.3.6' likely does not exist: build fails at dependency resolution",
      "severity": "high",
      "category": "external",
      "location": "gradle/libs.versions.toml:3",
      "description": "The version catalog declares ksp = \"2.3.6\". KSP 2.3.x uses a standalone versioning scheme (no longer embedding the Kotlin version), but KSP 2.3.x releases must still correspond to a published artifact on Maven Central and Gradle Plugin Portal. As of the design date, KSP artifacts following the new scheme are published as '2.x.y-1.0.z' or similar. A bare '2.3.6' may not resolve. The design itself flags this as assumption A2 ('verify compatibility before implementation'), but the implementation shipped the version without verification. If the artifact is absent, the root build fails at configuration time with 'Could not find com.google.devtools.ksp:symbol-processing-api:2.3.6'.",
      "evidence": "ksp = \"2.3.6\"\nksp = { id = \"com.google.devtools.ksp\", version.ref = \"ksp\" }",
      "scenario": "Any developer runs ./gradlew build on a fresh checkout. Gradle resolves plugins, fetches com.google.devtools.ksp:2.3.6, and fails with 'Could not resolve com.google.devtools.ksp:symbol-processing-gradle-plugin:2.3.6'. The alias(libs.plugins.ksp) apply false in root build.gradle.kts still triggers resolution. The build does not start."
    },
    {
      "id": "COR-07",
      "title": "user.dir path assumption breaks when spike tests run outside the Gradle test task",
      "severity": "medium",
      "category": "external",
      "location": "SpikeShader1DecisionSpec.kt:12 / SpikeShader2DecisionSpec.kt:11",
      "description": "Both spec files resolve the project root via Paths.get(System.getProperty(\"user.dir\")). The khaos-test-harness/build.gradle.kts sets workingDir = rootProject.projectDir, which causes Gradle's test forking to set user.dir to the project root before launching the test JVM. This is the only mechanism that makes the path resolution correct. If tests are run from an IDE (IntelliJ's 'Run test' button), user.dir is set by the IDE — typically the module root (khaos-test-harness/) or the project root depending on run configuration. If user.dir resolves to the module root, the path _bmad-output/planning-artifacts/designs/spike-shader-1-decision.md does not exist relative to it, decisionDoc.exists returns false, and all TCs that access decisionDoc.text fail with IllegalStateException.",
      "evidence": "private val projectRoot = Paths.get(System.getProperty(\"user.dir\"))\nprivate val decisionDoc = SpikeDecisionDocument(\n    projectRoot.resolve(\"_bmad-output/planning-artifacts/designs/spike-shader-1-decision.md\")\n)",
      "scenario": "Developer opens khaos-test-harness module in IntelliJ and clicks the gutter icon next to SpikeShader1DecisionSpec. IntelliJ sets working directory to the module root (/Users/clay/Development/khaos/khaos-test-harness/). decisionDoc.exists is false. TC-1 fails immediately with 'File must exist at _bmad-output/...' — but the file does exist, just at a different path. All subsequent TCs that call decisionDoc.text throw 'Decision document not found at khaos-test-harness/_bmad-output/...'."
    },
    {
      "id": "COR-08",
      "title": "git log given absolute path but working directory inside subdirectory: path may not be tracked by that repo root",
      "severity": "low",
      "category": "external",
      "location": "SpikeDecisionDocument.kt:18-19",
      "description": "git log --oneline -- <absolute_path> is invoked with working directory set to path.parent (the document's containing directory). Git discovers the repo root by walking up from path.parent. If path.parent is inside a git-submodule or a nested git repository (different root), git log may search the submodule's history rather than the parent repo's history, returning empty output even if the file is committed in the outer repo. committedToGit() returns false incorrectly.",
      "evidence": "ProcessBuilder(\"git\", \"log\", \"--oneline\", \"--\", path.toString())\n    .directory(path.parent.toFile())",
      "scenario": "_bmad-output/ is initialized as a separate git repository or submodule (possible if output artifacts are tracked separately). git discovers the inner repo from path.parent, finds no commits for the file in the inner history, returns empty output. committedToGit() returns false. All TC-1 assertions in both spec files fail: 'Document must be committed to version control'."
    },
    {
      "id": "COR-09",
      "title": "TC-13 anchor miss silently expands scope to full document",
      "severity": "low",
      "category": "boundary",
      "location": "SpikeShader1DecisionSpec.kt:219",
      "description": "SpikeShader1DecisionSpec TC-13 uses text.substringAfter(\"SpirvValidateTask\") to scope the isIgnoreExitValue = true check. String.substringAfter() returns the FULL string when the delimiter is absent, per Kotlin contract. If the document uses a different capitalization (e.g., 'SpirvValidate' or 'spirv_validate_task') or the section is renamed, the check silently degrades to asserting that the ENTIRE document does not contain isIgnoreExitValue = true. This is wider than intended and may incorrectly FAIL the test if isIgnoreExitValue = true appears in the ShaderCompileTask section with legitimate use.",
      "evidence": "val spirvValSection = text.substringAfter(\"SpirvValidateTask\")  // returns full text if not found\nspirvValSection shouldNotContain \"isIgnoreExitValue = true\"",
      "scenario": "The decision document names the task 'SpirvValidationTask' (different suffix). substringAfter returns the full text. The ShaderCompileTask section contains 'isIgnoreExitValue = true' (intentional for compile-phase error tolerance). The shouldNotContain assertion on the full text fails. The test incorrectly reports a violation that does not exist in the SPIRV validate section."
    },
    {
      "id": "COR-10",
      "title": "bodyFuture.get(5s) throws TimeoutException if stream reader is preempted after process exit",
      "severity": "low",
      "category": "concurrency",
      "location": "SpikeShader1DecisionSpec.kt:122 / SpikeShader2DecisionSpec.kt:113",
      "description": "In TC-5b (Shader1) and TC-5 (Shader2), the gh subprocess output is read by a CompletableFuture.supplyAsync thread from the common fork-join pool. After the main thread confirms exited == true, it calls bodyFuture.get(5, TimeUnit.SECONDS). If the FJP worker thread is starved (high system load, GC pause, other FJP tasks) and has not yet finished readText() within 5 seconds after process exit, bodyFuture.get() throws java.util.concurrent.TimeoutException. This exception propagates uncaught from the test lambda, producing a cryptic stack trace rather than a meaningful test failure message.",
      "evidence": "val bodyFuture = CompletableFuture.supplyAsync { result.inputStream.bufferedReader().readText() }\n// ... waitFor ...\nval body = bodyFuture.get(5, TimeUnit.SECONDS)  // TimeoutException if FJP worker is starved",
      "scenario": "CI runner is under heavy load (parallel builds, GC pressure). The fork-join pool worker reading gh's stdout is preempted. The process has exited (exited == true), but the stream hasn't reached EOF processing completion in the worker thread within 5 seconds. bodyFuture.get() throws TimeoutException. The test fails with a concurrent execution exception rather than a meaningful content assertion failure."
    },
    {
      "id": "COR-11",
      "title": "khaos.kotlin-kmp convention plugin lacks JaCoCo wiring: coverage gate will fail for all KMP modules",
      "severity": "medium",
      "category": "boundary",
      "location": "khaos.kotlin-kmp.gradle.kts (entire file)",
      "description": "The khaos.kotlin-kmp.gradle.kts convention plugin does not apply the jacoco plugin or configure JaCoCo for the jvmTest task. The shift-left-dev coverage gate runs diff-cover against a Cobertura XML or JaCoCo XML report. For KMP modules, no coverage data is ever generated. When the first KMP module receives real tests (Issue #4 onward), the coverage gate will fail silently (no report to compare against) or hard-fail (coverage tool errors on absent report). The design acknowledges this as assumption A6 (deferred), but it is a guaranteed failure for every KMP module that goes through the coverage gate.",
      "evidence": "// khaos.kotlin-kmp.gradle.kts — no jacoco plugin, no jacocoTestReport task\n// shift-left-dev coverage gate expects XML coverage report",
      "scenario": "Issue #4 (first KMP module implementation, e.g., khaos-core) is implemented. shift-left-dev runs coverage gate on khaos-core. No .exec file exists (jacoco not applied). diff-cover reports 0% coverage or fails to find the report XML. The coverage gate blocks merge even if all tests pass."
    }
  ]
}
```

---

## Summary

| ID | Severity | Category | Title |
|----|----------|----------|-------|
| COR-01 | high | concurrency | Timeout dead letter — readText() before waitFor() |
| COR-02 | medium | exception | IOException on missing parent directory |
| COR-03 | medium | concurrency | LazyThreadSafetyMode.NONE race on shared spec val |
| COR-04 | high | null | catalogLibs.findLibrary.get() — NoSuchElementException at configuration time |
| COR-05 | medium | exception | jacocoTestReport runs after failed test, obscures root cause |
| COR-06 | high | external | KSP version '2.3.6' likely absent from Maven Central |
| COR-07 | medium | external | user.dir path breaks under IDE test execution |
| COR-08 | low | external | git log inside nested repo returns wrong history |
| COR-09 | low | boundary | substringAfter anchor miss silently widens scope |
| COR-10 | low | concurrency | bodyFuture.get(5s) TimeoutException on starved FJP worker |
| COR-11 | medium | boundary | KMP modules have no JaCoCo wiring — coverage gate will fail |

**Critical paths to fix before merge:**

1. **COR-01** — Move `readText()` to a background thread (mirror the `CompletableFuture.supplyAsync` pattern already present in TC-5b). The timeout must fire while the read is ongoing, not after it completes.
2. **COR-04** — Replace `.get()` with `.orNull()` and an explicit null check, or use `orElseThrow { GradleException("Catalog entry '...' not found") }` with a named key in the message.
3. **COR-06** — Verify `ksp = "2.3.6"` resolves. If not, correct to the actual published version before this branch can build.
