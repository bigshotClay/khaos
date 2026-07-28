{
  "reviewer": "infiltrator",
  "loop": 2,
  "date": "2026-04-18",
  "scope": "Loop 2 delta only: lazy(NONE) change, TC-5 subprocess hardening, TC-16, TC-17 additions",
  "findings": [
    {
      "id": "INF-L2-01",
      "title": "Process not killed after waitFor timeout — orphaned subprocess retains GH_TOKEN in environment",
      "severity": "high",
      "category": "secrets",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:98-100",
      "description": "After waitFor(30, TimeUnit.SECONDS) returns false the subprocess is still alive. The code asserts exited shouldBe true (failing the test) but never calls result.destroy() or result.destroyForcibly(). The orphaned gh process continues to run with GH_TOKEN in its inherited environment, can complete the network call, and may log or forward the token to GitHub's API endpoint. There is no cleanup in a finally block or afterTest hook.",
      "evidence": "val exited = result.waitFor(30, TimeUnit.SECONDS)\nwithClue(\"gh subprocess must exit within 30 seconds\") { exited shouldBe true }\n// no result.destroy() — process continues running on timeout",
      "impact": "On network-slow CI, GH_TOKEN stays live in an orphaned process for an unbounded duration after the test suite moves on. Combined with it.clear() removing PATH and HOME, the gh process may misbehave in ways that expose the token in process table or OS audit logs."
    },
    {
      "id": "INF-L2-02",
      "title": "result.exitValue() called unconditionally after a possible timeout — throws IllegalThreadStateException leaking token via stack trace",
      "severity": "medium",
      "category": "secrets",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:100",
      "description": "result.exitValue() on a Process that has not yet terminated throws java.lang.IllegalThreadStateException. If waitFor returns false (timeout path), the assertion on line 99 fails but Kotest continues evaluating — it does NOT short-circuit the remaining withClue blocks in a soft-assertion context. When exitValue() then throws, the stack trace printed by the test runner contains the full ProcessBuilder command line, which may appear in CI logs that are accessible to other users. The GH_TOKEN value itself is not in the command line, but the timing exposes that the subprocess is still running with live credentials.",
      "evidence": "withClue(\"gh subprocess must exit with code 0\") { result.exitValue() shouldBe 0 } — called without first checking that exited == true",
      "impact": "Unhandled IllegalThreadStateException in CI log; orphaned process confirmed still running, increasing exposure window for the token."
    },
    {
      "id": "INF-L2-03",
      "title": "pb.environment().also { it.clear() } strips PATH — gh binary resolution becomes platform-dependent and potentially attacker-influenced",
      "severity": "medium",
      "category": "injection",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:95",
      "description": "it.clear() removes all environment variables including PATH before launching the gh child process. On Linux, without PATH the OS falls back to searching only /usr/local/bin:/usr/bin:/bin (POSIX default confstr _CS_PATH). On macOS the fallback is /usr/bin:/bin:/usr/sbin:/sbin. Any gh binary installed in a non-standard path (Homebrew at /opt/homebrew/bin, nix at /nix/store, custom CI runner at /home/runner/go/bin) will not be found, causing IOException at start() or a 'command not found' error. More importantly, if an attacker can place a binary named gh earlier in the POSIX fallback path than the real gh, they can intercept GH_TOKEN.",
      "evidence": "pb.environment().also { it.clear(); it[\"GH_TOKEN\"] = token } — only GH_TOKEN is set; PATH is absent",
      "impact": "On non-standard CI environments the subprocess silently fails or resolves gh from an unintended path. The GH_TOKEN is then presented to whatever binary is named gh at the fallback path."
    },
    {
      "id": "INF-L2-04",
      "title": "redirectErrorStream(true) in TC-5 merges gh error output into body — 'Gradle' in an error message satisfies the assertion",
      "severity": "medium",
      "category": "trust",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:94,101-103",
      "description": "With redirectErrorStream(true), stderr is merged into the same stream that body is read from. If gh fails (e.g., auth error, network timeout) and emits an error message such as 'error fetching issue: Gradle task endpoint unreachable', the string 'Gradle' appears in body and the final assertion body shouldContain \"Gradle\" passes. The exitValue() check on line 100 would catch a non-zero exit — but only if the process already exited. On timeout (exited=false) the exitValue check throws before reaching the content assertion, so the path where stderr provides a false positive is the fast-exit non-zero path, which is blocked. However, if gh exits 0 with an error message on stdout (a known gh quirk for some API errors), the false positive is live.",
      "evidence": "pb.redirectErrorStream(true) — merged stream fed into body; body shouldContain \"Gradle\" without verifying content is from stdout only",
      "impact": "AC 'SHADER-2 issue body references Gradle task' verified against potentially error-message content, not actual issue body."
    },
    {
      "id": "INF-L2-05",
      "title": "lazy(LazyThreadSafetyMode.NONE) on file-scope shared decisionDoc — data race in parallel Kotest execution",
      "severity": "medium",
      "category": "platform",
      "location": "src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:11, SpikeShader2DecisionSpec.kt:11-13",
      "description": "decisionDoc is declared as a file-level val shared across all specs in SpikeShader2DecisionSpec. The text property now uses LazyThreadSafetyMode.NONE, which uses UnsafeLazyImpl and provides no synchronization guarantee. If Kotest is configured with parallelism > 1 (or if a future test configuration enables it), two specs reading decisionDoc.text simultaneously can observe the lazy delegate in an intermediate state: one thread begins initializing (calls path.readText()), another thread observes _value as UNINITIALIZED and also calls path.readText(), and both can proceed with their own copies. This is the documented behavior of NONE. While the risk of incorrect test results is low for file reads, the change from the default SYNCHRONIZED mode is an intentional weakening of the safety guarantee on a shared object.",
      "evidence": "class SpikeDecisionDocument ... val text: String by lazy(LazyThreadSafetyMode.NONE) paired with private val decisionDoc = SpikeDecisionDocument(...) at file scope",
      "impact": "If parallel test execution is enabled, text may be initialized multiple times concurrently. For a file read this is benign but unnecessary. If the initializer is later replaced with a computation with side effects, the unsafety becomes a real hazard."
    },
    {
      "id": "INF-L2-06",
      "title": "TC-17 second-access assumption relies on UnsafeLazyImpl exception behavior — not guaranteed by Kotlin spec",
      "severity": "low",
      "category": "trust",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:252-266",
      "description": "TC-17 asserts that after the first access to absentDoc.text throws, the second access also throws — validating that lazy(NONE) does not cache the exception. This behavior is true of UnsafeLazyImpl in current Kotlin stdlib: a thrown exception leaves _value as UNINITIALIZED so the initializer re-runs. However, the Kotlin spec does not document this as a contract for LazyThreadSafetyMode.NONE; it is an implementation detail. A future Kotlin release could cache the exception (matching Java's ThreadLocal behavior) and TC-17 would break. The test validates undocumented implementation behavior, not a specified contract.",
      "evidence": "val secondError = runCatching { absentDoc.text }.exceptionOrNull()\nwithClue(\"Second access must also throw\") { ... shouldContain \"Decision document not found\" }",
      "impact": "Low. Test brittle against Kotlin stdlib internals. Not a security issue in isolation, but the design choice (NONE to avoid exception caching) is itself the footgun documented in INF-L2-05."
    },
    {
      "id": "INF-L2-07",
      "title": "INF-04 (hardcoded repo slug) not addressed in loop 2 — fork CI still validates upstream issue body",
      "severity": "medium",
      "category": "trust",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:92",
      "description": "The loop 1 finding INF-04 was carried over without remediation. 'bigshotClay/khaos' remains hardcoded. The loop 2 changes to TC-5 (environment scoping, timeout) do not address this. In a fork-based CI workflow, TC-5 reads the upstream repo's issue #17, not the fork's. An upstream maintainer can manipulate issue #17 to force-pass or force-fail TC-5 in any fork's CI run.",
      "evidence": "\"--repo\", \"bigshotClay/khaos\" — unchanged from loop 1",
      "impact": "Fork CI trusts upstream-controlled content. The hardcoded slug also bypasses any branch protection that would otherwise prevent external contributors from influencing test outcomes."
    },
    {
      "id": "INF-L2-08",
      "title": "INF-06 (redirectErrorStream in committedToGit) not addressed — git warnings still satisfy isNotBlank()",
      "severity": "medium",
      "category": "trust",
      "location": "src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:19",
      "description": "The loop 2 changes did not touch committedToGit(). redirectErrorStream(true) is still present on the git subprocess, and the return condition is still output.isNotBlank(). Any git warning (safe.directory advisory, 'dubious ownership' warning on CI, 'warning: LF will be replaced') emitted to stderr satisfies the non-blank check and causes committedToGit() to return true even when the file has zero git history. This finding was raised in loop 1 (INF-06) and remains open.",
      "evidence": ".redirectErrorStream(true) ... return output.isNotBlank() — git warnings on stderr = non-blank output = true",
      "impact": "TC-1 'document must be committed to git' passes vacuously in any CI environment that emits git safe.directory or ownership warnings."
    },
    {
      "id": "INF-L2-09",
      "title": "INF-07 (no timeout on committedToGit waitFor) not addressed in loop 2",
      "severity": "medium",
      "category": "platform",
      "location": "src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:22",
      "description": "Loop 2 added waitFor(30, TimeUnit.SECONDS) to the gh subprocess in TC-5 but did not add a timeout to the git subprocess in committedToGit(). result.waitFor() with no arguments blocks indefinitely. In a CI environment where git requires interactive SSH agent authentication or where the git binary hangs on a credential prompt, TC-1 blocks the entire JVM indefinitely.",
      "evidence": "result.waitFor() — bare call with no timeout, unchanged from loop 1",
      "impact": "CI pipeline hangs until job-level hard timeout. Partially addressed in TC-5 only."
    }
  ]
}
