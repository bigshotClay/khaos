{
  "reviewer": "coroner",
  "loop": 2,
  "findings": [
    {
      "id": "COR-L2-01",
      "title": "TC-5: readText() before waitFor(timeout) — timeout is unreachable when gh hangs without closing stdout",
      "severity": "critical",
      "category": "concurrency",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:97-98",
      "description": "redirectErrorStream(true) merges stderr into stdout, which is correct. However, result.inputStream.bufferedReader().readText() is called on line 97 BEFORE result.waitFor(30, TimeUnit.SECONDS) on line 98. readText() reads until EOF. EOF on the stream is not delivered until the child process closes its stdout file descriptor, which only happens when the process exits. If gh hangs — network freeze, DNS timeout, stalled TLS handshake, or a credential prompt that blocks on a non-existent tty — the child never exits, stdout never reaches EOF, readText() blocks indefinitely on the calling thread, and waitFor(30, TimeUnit.SECONDS) on line 98 is never reached. The 30-second timeout on line 98 provides zero protection. The test plan constraint 'a timeout on waitFor(30, TimeUnit.SECONDS) to prevent CI deadlock' is satisfied syntactically but defeated by execution order.",
      "evidence": "Line 97: val body = result.inputStream.bufferedReader().readText()\nLine 98: val exited = result.waitFor(30, TimeUnit.SECONDS)\nreadText() blocks until stream EOF. EOF fires on process exit. If process does not exit, EOF never fires, readText() never returns, waitFor is never called.",
      "scenario": "CI runner has no outbound HTTPS to api.github.com (network policy, firewall, airgapped runner). gh opens a TCP connection that stalls in SYN_SENT indefinitely. readText() blocks. The Gradle test task hangs. CI job runs until the hard job timeout (typically 6 hours), burning runner minutes. The 30-second guard on waitFor never fires because execution never reaches that line."
    },
    {
      "id": "COR-L2-02",
      "title": "TC-5: body is read after process may have already timed out — exitValue() can throw IllegalThreadStateException",
      "severity": "high",
      "category": "boundary",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:98-100",
      "description": "Even if readText() does return (the normal case where gh runs and exits), the sequence is: readText() blocks until process exits (consuming all output), then waitFor(30, TimeUnit.SECONDS) is called on an already-dead process. waitFor on an already-exited process returns true immediately, so exited is always true in the non-hang path. This means the withClue('gh subprocess must exit within 30 seconds') assertion on line 99 is vacuously true — it cannot distinguish between 'process exited within 30 seconds' and 'process had already exited before waitFor was even called'. The constraint the test plan requires is not actually tested.",
      "evidence": "readText() drains the stream, which requires process exit. After readText() returns, the process is guaranteed to have already exited. waitFor(30, TimeUnit.SECONDS) on an exited process returns true immediately regardless of how long the process actually took.",
      "scenario": "gh takes 45 seconds and then exits. readText() blocks for 45 seconds, then returns. waitFor(30, TimeUnit.SECONDS) returns true immediately (process already dead). The withClue assertion passes. The test plan's 30-second deadline was violated, but the test does not detect it."
    },
    {
      "id": "COR-L2-03",
      "title": "lazy(LazyThreadSafetyMode.NONE) does NOT prevent exception caching — but neither does default lazy; loop-1 finding COR-02 was factually wrong",
      "severity": "info",
      "category": "exception",
      "location": "src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:11",
      "description": "The loop-1 shadow finding (and COR-02) claimed that Kotlin's default lazy caches and re-throws exceptions, and that LazyThreadSafetyMode.NONE was needed to prevent this. This is factually incorrect. Neither SynchronizedLazyImpl (default) nor UnsafeLazyImpl (NONE mode) caches exceptions. Both implementations store the computed value in _value, which remains at the UNINITIALIZED_VALUE sentinel if the initializer throws. On the next access the initializer runs again. TC-17 correctly validates retry behavior, but that behavior exists identically with plain lazy {}. The change from lazy to lazy(LazyThreadSafetyMode.NONE) is semantically neutral with respect to exception behavior; its only effect is removing the unnecessary double-checked locking overhead. The fix addressed the right symptom for the wrong reason. TC-17 is still a valid test — it correctly pins the retry contract — but the premise in the shadow document is wrong.",
      "evidence": "UnsafeLazyImpl source: _value starts as UNINITIALIZED_VALUE. The initializer block is called in getValue() only when _value === UNINITIALIZED_VALUE. No try/catch wraps the initializer — exceptions escape without storing to _value. SynchronizedLazyImpl uses the same sentinel pattern with a lock. Neither implementation has exception-caching code. Kotlin issue KT-9748 (request to cache exceptions in lazy) was WONTFIX.",
      "scenario": "Not a regression. The LazyThreadSafetyMode.NONE change is harmless and TC-17 is correct behavior verification. However, if future maintainers revert to plain lazy {} believing exception caching will resume, they will be surprised to find the same retry behavior persists — potentially leading to a different 'fix' that breaks something else."
    },
    {
      "id": "COR-L2-04",
      "title": "TC-17: second access test relies on lazy retry — but check(exists) re-evaluates path.exists() which can change between calls",
      "severity": "medium",
      "category": "boundary",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:252-266",
      "description": "TC-17 constructs absentDoc with a path that does not exist, then calls absentDoc.text twice and asserts both throws contain 'Decision document not found'. The test is correct for the isolated case. However, the lazy initializer calls check(exists) where exists is a computed property that calls path.exists() live each time the lazy is retried. If another thread or process creates the file between the first and second access (race with a parallel test, Gradle worker, or OS temporary file creation with the same name), the second access would succeed and return content — TC-17 would fail with 'no exception' on the second call. The test name says 'no lazy exception caching cascade' but the real invariant being tested is 'initializer runs fresh each time', which depends on file-system state at call time.",
      "evidence": "val exists: Boolean get() = path.exists() — live re-evaluation on each call. lazy initializer: check(exists) re-calls path.exists() on each retry. File name 'does-not-exist-spike-shader-2.md' is predictable.",
      "scenario": "Parallel test run with Gradle --parallel. Another test creates a file named 'does-not-exist-spike-shader-2.md' as a side effect (unlikely but possible in chaotic temp-file scenarios). TC-17 second access returns content instead of throwing, assertion fails with 'no exception' clue."
    },
    {
      "id": "COR-L2-05",
      "title": "TC-5 environment clear removes PATH — gh binary may not be found after pb.environment().clear()",
      "severity": "high",
      "category": "platform",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:95",
      "description": "pb.environment().also { it.clear(); it[\"GH_TOKEN\"] = token } clears the entire inherited environment and sets only GH_TOKEN. ProcessBuilder resolves the executable ('gh') using the PATH environment variable. After clearing the environment, PATH is absent. On Linux and macOS, ProcessBuilder with no PATH falls back to a minimal default search path (/usr/bin, /bin on some JVM implementations) or throws IOException if 'gh' is not in those paths. gh is typically installed to /usr/local/bin (Homebrew), /opt/homebrew/bin (Apple Silicon Homebrew), or /home/linuxbrew/.linuxbrew/bin (CI Homebrew). None of these are in the fallback search path. The process will throw IOException: No such file or directory (ENOENT) rather than failing with a meaningful test assertion.",
      "evidence": "it.clear() removes all environment variables including PATH. The command is 'gh' (not an absolute path). ProcessBuilder uses execvpe() on Linux which requires PATH to find non-absolute executables. Java's ProcessBuilder on Linux uses /bin/sh -c only for shell=true; with a list-form command it uses execve() directly, which requires absolute path or PATH.",
      "scenario": "CI runner (GitHub Actions ubuntu-latest) has gh at /usr/bin/gh (standard for actions/setup-gh) — this may be in the fallback path and succeed. But on macOS runners or custom Docker images with Homebrew-installed gh, the clear() causes IOException 'No such file or directory', the exception propagates uncaught out of the test, and the failure message gives no indication that the gh binary was found but PATH was cleared."
    },
    {
      "id": "COR-L2-06",
      "title": "committedToGit() still has no timeout on waitFor() — loop-2 did not address the hang risk in this method",
      "severity": "medium",
      "category": "concurrency",
      "location": "src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:22",
      "description": "The loop-2 changes correctly added redirectErrorStream(true) to TC-5 and a 30-second waitFor timeout to TC-5. The committedToGit() method in SpikeDecisionDocument already had redirectErrorStream(true) from loop-1, but still uses result.waitFor() with no timeout. Git operations (particularly git log over a networked filesystem, or with SSH remote tracking) can block. The test plan shadow noted subprocess hangs but the fix was only applied to TC-5's inline ProcessBuilder; committedToGit() was not updated. TC-1 calls committedToGit() and can still hang indefinitely.",
      "evidence": "SpikeDecisionDocument.kt line 22: result.waitFor() — overload without (long, TimeUnit). Loop-2 diff updated TC-5's inline subprocess but left committedToGit() unchanged.",
      "scenario": "CI runs on a host where git index files are on NFS. git log acquires a read lock that blocks on NFS timeout (default 90 seconds per operation, can retry). waitFor() blocks. TC-1 hangs the Gradle test task."
    },
    {
      "id": "COR-L2-07",
      "title": "TC-5 body read after process hang scenario: body is empty string, not null — all three assertions run regardless",
      "severity": "low",
      "category": "boundary",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:97-103",
      "description": "If gh exits non-zero (network error, auth failure, rate limit), readText() returns whatever partial output was written before exit — which may be an error message in stdout (because redirectErrorStream(true) merges stderr into stdout). The body variable then contains gh's error prose ('Could not resolve to a Repository with the name...'), not the issue body JSON. The assertion 'result.exitValue() shouldBe 0' on line 100 fires correctly. But the 'body shouldContain Gradle' assertion on line 102 also runs and produces a second failure with a message like 'Expected \"error: Could not resolve...\" to contain \"Gradle\"' — two failures from one root cause, the second being misleading.",
      "evidence": "withClue assertions are sequential, not guarded by prior assertion success. Kotest ShouldSpec continues executing assertions after a withClue failure in soft-assertion mode, or both fire before the first is reported in hard mode.",
      "scenario": "Token has expired. gh exits 1 with 'HTTP 401: Bad credentials' in stdout (due to redirectErrorStream). exitValue assertion fires correctly. body assertion also fires with confusing 'Expected HTTP 401 error to contain Gradle'."
    }
  ]
}
