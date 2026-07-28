{
  "reviewer": "infiltrator",
  "loop": 2,
  "date": "2026-04-19",
  "scope": "Loop 2 (second pass): new findings + open carry-forwards from 2026-04-18 loop 2. Previous Gauntlet loop 1 addressed: TC-5 subprocess constraints, TC-16 error handling, TC-17 lazy cascade. This pass finds 3 new issues and confirms 9 prior findings remain unaddressed.",
  "findings": [
    {
      "id": "INF-L2B-01",
      "title": "user.dir system property controls projectRoot — full test bypass via -Duser.dir",
      "severity": "high",
      "category": "injection",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:10, SpikeShader1DecisionSpec.kt:10",
      "description": "Both spec files derive projectRoot from System.getProperty(\"user.dir\"), a JVM system property that can be overridden at test execution time via the -D flag, JAVA_TOOL_OPTIONS, or gradle.properties (systemProp.user.dir). Overriding user.dir redirects every file-system assertion — TC-1 through TC-17 — to read from an attacker-controlled directory containing a crafted decision document that satisfies all checks. The path is never validated against the real working tree. This is a trust boundary violation: the test suite assumes user.dir is the project root, but any caller of ./gradlew test controls that assumption.",
      "evidence": "private val projectRoot = Paths.get(System.getProperty(\"user.dir\"))\nprivate val decisionDoc = SpikeDecisionDocument(\n    projectRoot.resolve(\"_bmad-output/planning-artifacts/designs/spike-shader-2-decision.md\")\n)",
      "impact": "An attacker who controls JVM arguments (e.g., a malicious Gradle plugin, a CI runner misconfiguration, or a JAVA_TOOL_OPTIONS value set in the environment) can substitute an arbitrary document that satisfies every acceptance criterion. All TCs pass against fabricated content; the actual decision document is never read. This completely breaks the acceptance test guarantee."
    },
    {
      "id": "INF-L2B-02",
      "title": "TC-5b (SpikeShader1DecisionSpec) subprocess not hardened — timeout, env isolation, and exit check all missing",
      "severity": "high",
      "category": "secrets",
      "location": "src/test/kotlin/khaos/spike/SpikeShader1DecisionSpec.kt:100-108",
      "description": "TC-5 in SpikeShader2DecisionSpec received subprocess hardening in Loop 2: environment.clear() + GH_TOKEN isolation, 30-second waitFor timeout, and exitValue() verification. None of that hardening was backported to TC-5b in SpikeShader1DecisionSpec. TC-5b still: (1) inherits the full parent process environment including any ambient credentials, proxy tokens, or sensitive env vars; (2) calls waitFor() with no timeout, blocking indefinitely on a stalled gh process; (3) never checks exitValue(), so gh failure is silently ignored and the test passes vacuously; (4) uses redirectErrorStream(false), discarding gh error output so authentication failures are invisible. The two test files run in the same JVM test execution context — the security disparity between TC-5 and TC-5b is a regression introduced by the Loop 2 hardening that patched only one of the two call sites.",
      "evidence": "val result = ProcessBuilder(\"gh\", \"issue\", \"view\", \"16\",\n    \"--repo\", \"bigshotClay/khaos\", \"--json\", \"body\", \"--jq\", \".body\")\n    .redirectErrorStream(false)\n    .start()\nval body = result.inputStream.bufferedReader().readText()\nresult.waitFor()  // no timeout\n// no exitValue() check — failure silently ignored",
      "impact": "GH_TOKEN (from the ambient environment) is passed to gh without isolation. A malicious binary named gh earlier on PATH receives the full unscoped token. The indefinite waitFor() hangs the entire test suite. Silent gh failure means TC-5b passes even if the issue body was never fetched, providing false confidence that SHADER-1 was updated."
    },
    {
      "id": "INF-L2B-03",
      "title": "JaCoCo plugin version unpinned — Gradle-bundled version used without explicit constraint",
      "severity": "low",
      "category": "platform",
      "location": "build.gradle.kts:3",
      "description": "The jacoco plugin is applied with no version specified: `plugins { jacoco }`. Gradle resolves this to the JaCoCo version bundled with the Gradle distribution being used, which varies by Gradle version and is not explicitly recorded anywhere in the project. A Gradle wrapper upgrade silently changes the JaCoCo version. While no critical CVEs are active against current bundled versions, an unpinned toolchain version violates supply chain hygiene: reproducible builds require pinned tool versions. The XML report output path is deterministic, but the report content and bytecode instrumentation behavior can differ across JaCoCo versions.",
      "evidence": "plugins {\n    kotlin(\"jvm\") version \"2.1.20\"\n    jacoco  // no version — uses Gradle-bundled JaCoCo\n}",
      "impact": "Gradle version upgrade silently changes JaCoCo behavior. Low immediate risk since jacoco is a test reporter only; no production code is instrumented. Escalates if the project adds instrumented builds or if a future JaCoCo version introduces a behavioral regression in the XML report format consumed by CI tooling."
    },
    {
      "id": "INF-L2-01",
      "title": "CARRY-FORWARD: Process not killed after waitFor timeout — orphaned subprocess retains GH_TOKEN",
      "severity": "high",
      "category": "secrets",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:98-100",
      "description": "After waitFor(30, TimeUnit.SECONDS) returns false the subprocess is still alive. The code asserts exited shouldBe true (failing the test) but never calls result.destroy() or result.destroyForcibly(). The orphaned gh process continues to run with GH_TOKEN in its environment. No cleanup in a finally block or afterTest hook. Not addressed in this PR.",
      "evidence": "val exited = result.waitFor(30, TimeUnit.SECONDS)\nwithClue(\"gh subprocess must exit within 30 seconds\") { exited shouldBe true }\n// no result.destroy() — process continues running on timeout",
      "impact": "On network-slow CI, GH_TOKEN stays live in an orphaned process for an unbounded duration after the test suite moves on. Combined with it.clear() removing PATH and HOME, the gh process may misbehave in ways that expose the token in process table or OS audit logs."
    },
    {
      "id": "INF-L2-02",
      "title": "CARRY-FORWARD: result.exitValue() called unconditionally after possible timeout — IllegalThreadStateException leaks subprocess state",
      "severity": "medium",
      "category": "secrets",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:100",
      "description": "result.exitValue() on a Process that has not yet terminated throws IllegalThreadStateException. If waitFor returns false, the assertion on line 99 fails but the remaining withClue block still executes in Kotest's default (non-soft) assertion mode. The stack trace printed by the test runner contains the full ProcessBuilder command line. Not addressed in this PR.",
      "evidence": "withClue(\"gh subprocess must exit with code 0\") { result.exitValue() shouldBe 0 } — called without first checking that exited == true",
      "impact": "Unhandled IllegalThreadStateException in CI log; orphaned process confirmed still running, increasing exposure window for the token."
    },
    {
      "id": "INF-L2-03",
      "title": "CARRY-FORWARD: pb.environment().clear() strips PATH — gh binary resolution becomes platform-dependent",
      "severity": "medium",
      "category": "injection",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:95",
      "description": "it.clear() removes all environment variables including PATH before launching gh. On Linux, POSIX default confstr _CS_PATH (/usr/local/bin:/usr/bin:/bin) applies. On macOS the fallback is /usr/bin:/bin:/usr/sbin:/sbin. Any gh installation outside those paths (Homebrew /opt/homebrew/bin, nix /nix/store, CI runner /home/runner/go/bin) is not found. More critically, an attacker who can place a binary named gh earlier in the POSIX fallback path receives GH_TOKEN. Not addressed in this PR.",
      "evidence": "pb.environment().also { it.clear(); it[\"GH_TOKEN\"] = token } — only GH_TOKEN set; PATH absent",
      "impact": "On non-standard CI environments the subprocess silently fails or resolves gh from an unintended binary. GH_TOKEN is presented to whatever binary is named gh at the POSIX fallback path."
    },
    {
      "id": "INF-L2-04",
      "title": "CARRY-FORWARD: redirectErrorStream(true) in TC-5 — gh error output can satisfy content assertion",
      "severity": "medium",
      "category": "trust",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:94,101-103",
      "description": "With redirectErrorStream(true), stderr is merged into the stream that body is read from. If gh emits an error message containing 'Gradle' (e.g., 'error fetching issue: Gradle API endpoint unavailable'), the content assertion body shouldContain \"Gradle\" passes against error output, not the issue body. Not addressed in this PR.",
      "evidence": "pb.redirectErrorStream(true) — merged stream fed into body; body shouldContain \"Gradle\" without verifying content is from stdout only",
      "impact": "AC 'SHADER-2 issue body references Gradle task' verified against potentially error-message content, not actual issue body."
    },
    {
      "id": "INF-L2-05",
      "title": "CARRY-FORWARD: lazy(LazyThreadSafetyMode.NONE) on file-scope shared decisionDoc — data race under parallel Kotest execution",
      "severity": "medium",
      "category": "platform",
      "location": "src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:11, SpikeShader2DecisionSpec.kt:11-13",
      "description": "decisionDoc is a file-level val shared across all specs. The text property now uses LazyThreadSafetyMode.NONE (UnsafeLazyImpl). If Kotest parallelism > 1 is enabled, two specs reading decisionDoc.text simultaneously can observe the lazy delegate in an intermediate state. This is the documented behavior of NONE — the change from SYNCHRONIZED is an intentional weakening of the safety guarantee on a shared object. The change was made in this PR and is an unaddressed risk.",
      "evidence": "class SpikeDecisionDocument ... val text: String by lazy(LazyThreadSafetyMode.NONE) paired with private val decisionDoc = SpikeDecisionDocument(...) at file scope",
      "impact": "If parallel test execution is enabled, text may be initialized multiple times concurrently. For a file read this is currently benign. If the initializer is later replaced with a computation with side effects, the unsafety is a real hazard."
    },
    {
      "id": "INF-L2-06",
      "title": "CARRY-FORWARD: TC-17 second-access assumption relies on UnsafeLazyImpl exception behavior — not guaranteed by Kotlin spec",
      "severity": "low",
      "category": "trust",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:252-266",
      "description": "TC-17 asserts that after the first access to absentDoc.text throws, the second access also throws — validating that lazy(NONE) does not cache the exception. This is true of UnsafeLazyImpl in current Kotlin stdlib (thrown exception leaves _value as UNINITIALIZED). However, the Kotlin spec does not document this as a contract for LazyThreadSafetyMode.NONE. A future Kotlin release could cache the exception and TC-17 would break. The test validates undocumented implementation behavior.",
      "evidence": "val secondError = runCatching { absentDoc.text }.exceptionOrNull()\nwithClue(\"Second access must also throw\") { ... shouldContain \"Decision document not found\" }",
      "impact": "Low. Test brittle against Kotlin stdlib internals. The design choice (NONE to avoid exception caching) is itself the footgun documented in INF-L2-05."
    },
    {
      "id": "INF-L2-07",
      "title": "CARRY-FORWARD: hardcoded repo slug 'bigshotClay/khaos' — fork CI validates upstream-controlled content",
      "severity": "medium",
      "category": "trust",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:92",
      "description": "The loop 1 finding INF-04 was not addressed in loop 2. 'bigshotClay/khaos' remains hardcoded. In a fork-based CI workflow, TC-5 reads the upstream repo's issue #17, not the fork's. An upstream maintainer can manipulate issue #17 to force-pass or force-fail TC-5 in any fork's CI run.",
      "evidence": "\"--repo\", \"bigshotClay/khaos\" — hardcoded slug unchanged",
      "impact": "Fork CI trusts upstream-controlled content. The hardcoded slug bypasses any branch protection that would otherwise prevent external contributors from influencing test outcomes."
    },
    {
      "id": "INF-L2-08",
      "title": "CARRY-FORWARD: redirectErrorStream(true) in committedToGit() — git warnings satisfy isNotBlank()",
      "severity": "medium",
      "category": "trust",
      "location": "src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:19",
      "description": "committedToGit() was not touched in this PR. redirectErrorStream(true) is still present on the git subprocess, and the return condition is still output.isNotBlank(). Any git warning (safe.directory advisory, 'dubious ownership' on CI, 'warning: LF will be replaced') emitted to stderr satisfies the non-blank check and causes committedToGit() to return true even when the file has zero git history.",
      "evidence": ".redirectErrorStream(true) ... return output.isNotBlank() — git warnings on stderr = non-blank output = true",
      "impact": "TC-1 'document must be committed to git' passes vacuously in any CI environment that emits git safe.directory or ownership warnings."
    },
    {
      "id": "INF-L2-09",
      "title": "CARRY-FORWARD: no timeout on committedToGit() waitFor — indefinite JVM block",
      "severity": "medium",
      "category": "platform",
      "location": "src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:22",
      "description": "Loop 2 added waitFor(30, TimeUnit.SECONDS) to the gh subprocess in TC-5 but did not add a timeout to the git subprocess in committedToGit(). result.waitFor() with no arguments blocks indefinitely. In a CI environment where git requires interactive SSH agent authentication or where the git binary hangs on a credential prompt, TC-1 blocks the entire JVM indefinitely.",
      "evidence": "result.waitFor() — bare call with no timeout, unchanged from loop 1",
      "impact": "CI pipeline hangs until job-level hard timeout. Partially addressed in TC-5 only; committedToGit() remains vulnerable."
    }
  ]
}
