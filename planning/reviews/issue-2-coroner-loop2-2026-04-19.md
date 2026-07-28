{
  "reviewer": "coroner",
  "loop": "2-2026-04-19",
  "scope": "Post-fix review. Loop-1 Gauntlet raised subprocess deadlock (TC-5), lazy exception caching (TC-17), error handling silence (TC-16). Fix commit 214cb25 addressed TC-16/TC-17 and partially addressed TC-5 (added redirectErrorStream, timeout, exit-code check). This pass audits what remains broken and what is newly introduced.",
  "findings": [
    {
      "id": "COR-L3-01",
      "title": "TC-5: 30-second timeout is architecturally inert — readText() is the actual blocking gate",
      "severity": "critical",
      "category": "concurrency",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:97-98",
      "description": "The fix added waitFor(30, TimeUnit.SECONDS) to prevent CI deadlock, but left readText() before waitFor(). readText() calls InputStream.read() in a loop until it receives EOF (-1). EOF on a pipe is delivered only when the child process closes its stdout file descriptor, which happens only when the process exits. If gh hangs — network stall, stalled TLS handshake, blocked DNS — the child never exits, EOF never arrives, readText() blocks indefinitely on the calling thread, and the line containing waitFor(30, TimeUnit.SECONDS) is never reached. The timeout guard is syntactically present but execution-order defeated. The fix from commit 214cb25 corrected the stderr deadlock (COR-03 from loop-1) by adding redirectErrorStream(true), but did not fix the execution order. This is COR-L2-01 from the previous loop, confirmed still present and still unfixed.",
      "evidence": "Line 97: val body = result.inputStream.bufferedReader().readText() — blocks until process exits.\nLine 98: val exited = result.waitFor(30, TimeUnit.SECONDS) — unreachable if line 97 hangs.\nreadText() is defined as: 'Reads this stream completely as a String.' The stream is a pipe; pipes only signal EOF on write-end close; write-end close occurs on process exit.",
      "scenario": "GitHub Actions runner loses outbound connectivity to api.github.com mid-request (network policy change, rate-limit TCP RST, or NAT gateway timeout). gh holds the socket open waiting for the response. readText() blocks. The Gradle test JVM hangs. The job runs until its 6-hour hard timeout. The 30-second guard on waitFor never fires."
    },
    {
      "id": "COR-L3-02",
      "title": "TC-5: env.clear() removes HOME — gh cannot locate its config directory and may fail authentication",
      "severity": "high",
      "category": "platform",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:95",
      "description": "pb.environment().also { it.clear(); it[\"GH_TOKEN\"] = token } clears the entire process environment and sets only GH_TOKEN. The gh CLI resolves its configuration directory via $GH_CONFIG_DIR, falling back to $HOME/.config/gh/. Without HOME in the environment, gh's Go runtime calls os.UserHomeDir(), which on Linux falls back to looking up the passwd entry for the current UID. On minimal CI containers this may return '/' or fail, causing gh to report 'You are not logged in' even though GH_TOKEN is set. On macOS, the Keychain integration for token storage may also fail without HOME. The symptom is an authentication error that is distinct from a bad token, and the withClue message 'gh subprocess must exit with code 0' will fire without any indication that the root cause is a missing HOME variable. This is distinct from and compounds COR-L2-05 (PATH removal) from the previous loop.",
      "evidence": "it.clear() removes HOME, USER, TMPDIR, SSL_CERT_FILE, GH_CONFIG_DIR, and all other inherited vars.\ngh source (github.com/cli/cli): config.ConfigDir() checks $GH_CONFIG_DIR then filepath.Join(os.UserHomeDir(), \".config\", \"gh\").\nos.UserHomeDir() on Linux: checks $HOME; if absent, reads /etc/passwd entry for current UID; may return '/' on scratch containers.",
      "scenario": "GitHub Actions ubuntu-latest runner. gh is installed at /usr/bin/gh (in minimal fallback path, so ENOENT is avoided). But HOME is cleared. gh cannot locate ~/.config/gh/. The token passed via GH_TOKEN is used, but gh also checks the host configuration for api endpoint overrides. If the config parse fails, gh exits 1. The test fails with exit-code assertion, not PATH error — misleading diagnosis."
    },
    {
      "id": "COR-L3-03",
      "title": "committedToGit(): git exit code is discarded — non-zero git error with non-blank output returns true",
      "severity": "high",
      "category": "boundary",
      "location": "src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:22-23",
      "description": "committedToGit() reads stdout and returns output.isNotBlank(). It does not check result.waitFor() return value or result.exitValue(). When git exits non-zero — 'fatal: not a git repository', 'fatal: bad object', 'error: pathspec did not match' — git writes the error message to stderr, which is merged into stdout by redirectErrorStream(true). The error message is non-blank, so the method returns true, incorrectly reporting the file as committed. TC-1 then passes despite the file never having been committed to git. This is COR-06 from loop-1, still present and unfixed through loops 1 and 2.",
      "evidence": "result.waitFor() return value unused.\nresult.exitValue() never called.\nreturn output.isNotBlank() — success criterion is byte presence, not command success.\ngit log on a path outside the tracked tree exits 0 with empty output (false negative).\ngit log in a non-git directory exits 128 with 'fatal: not a git repository' on stderr (false positive after merge).",
      "scenario": "_bmad-output/ is a git submodule that was added but not initialized (submodule directory exists as empty folder). git log run from the designs/ directory inside the submodule exits 128 with 'fatal: not a git repository'. redirectErrorStream merges this to stdout. output.isNotBlank() returns true. TC-1 passes, incorrectly asserting the file is committed."
    },
    {
      "id": "COR-L3-04",
      "title": "committedToGit(): no timeout on waitFor() — TC-1 can hang indefinitely",
      "severity": "medium",
      "category": "concurrency",
      "location": "src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:21-22",
      "description": "The fix in commit 214cb25 added a 30-second timeout to TC-5's inline ProcessBuilder, but committedToGit() in SpikeDecisionDocument still uses result.waitFor() with no timeout. git log is typically fast, but on NFS-mounted home directories, git index lock acquisition can block. On a CI runner with a stalled NFS mount, git hangs indefinitely. TC-1 calls committedToGit() directly and can deadlock the test task. Unlike TC-5, there is no skip guard and no token guard — TC-1 always runs. This is COR-L2-06 from the previous loop, still present.",
      "evidence": "SpikeDecisionDocument.kt line 22: result.waitFor() — overload without (long, TimeUnit) timeout argument.\nNo try/catch, no finally block, no interrupt handling.",
      "scenario": "CI runner with NFS-backed /home (e.g., on-premise Jenkins agent). git log acquires a read lock. NFS server becomes unresponsive. git blocks on lock acquisition. waitFor() blocks indefinitely. TC-1 hangs the Gradle test task."
    },
    {
      "id": "COR-L3-05",
      "title": "TC-5: waitFor returns false path — exitValue() throws IllegalThreadStateException before withClue fires, only when soft-assertion mode is active",
      "severity": "low",
      "category": "exception",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:98-100",
      "description": "In normal Kotest hard-assertion mode: if waitFor(30s) returns false (timeout), line 99 throws AssertionError before line 100 is reached, so result.exitValue() is never called on the still-running process. However, the process is never destroyed — no result.destroyForcibly() call exists anywhere in the test. In hard mode this is benign because readText() already guaranteed process exit (see COR-L3-01). If the test code is ever refactored to use shouldSpec's soft-assertion mode (via assertSoftly or ShouldSpec with softAssertions enabled), line 100 would be reached on the timeout path, and result.exitValue() on a still-running process throws java.lang.IllegalThreadStateException — an unchecked exception that is not an AssertionError. Kotest would report it as a test error (not a test failure), with a raw stack trace and no withClue context, making diagnosis harder. The absence of destroyForcibly() also means the process persists as an orphan if this path is ever reached.",
      "evidence": "Process.exitValue() javadoc: 'throws IllegalThreadStateException — if the subprocess represented by this Process object has not yet terminated'.\nNo result.destroyForcibly() call in TC-5 or in any finally block.\nNo assertSoftly wrapper present today, but ShouldSpec supports it.",
      "scenario": "Future refactor wraps TC-5 assertions in assertSoftly {} to collect all failures at once. waitFor returns false. Line 99 records the assertion failure (soft mode). Line 100 executes. exitValue() throws IllegalThreadStateException. Test reports both an assertion failure and an unexpected exception. The exception message 'process has not terminated' masks the real failure message from withClue."
    },
    {
      "id": "COR-L3-06",
      "title": "TC-5: body is read from merged stderr/stdout — gh error prose triggers both exit-code and content assertions",
      "severity": "low",
      "category": "boundary",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:97-103",
      "description": "redirectErrorStream(true) merges stderr into stdout. When gh fails (auth error, rate limit, network error), it writes its error message to stderr. After merging, body contains the error message prose. The exit-code assertion on line 100 fires correctly. But the content assertion on line 102 (body shouldContain 'Gradle') also executes and generates a second, confusing failure: 'Expected \"HTTP 401: Bad credentials\" to contain \"Gradle\"'. Two assertion failures from one root cause. The exit-code failure is the real signal; the content failure is noise that misleads diagnosis. This is COR-L2-07 from the previous loop, still present.",
      "evidence": "withClue assertions are sequential and independent — each throws AssertionError on failure.\nLine 100 and line 102 both execute regardless of prior assertion state (hard mode throws on line 100, preventing line 102; but in soft mode both fire).\nIn hard mode: line 100 fires, test aborts, line 102 never fires — the confusing failure only materializes in soft mode. In the current hard-mode setup this is low severity.",
      "scenario": "GH_TOKEN is present but expired. gh exits 1 with '401: Bad credentials' in stderr (merged to stdout). Line 100 assertion throws AssertionError. Test reported as failed with exit code assertion. Diagnosis is clear in hard mode. If soft assertions are ever added, the spurious 'Gradle' failure appears and misleads."
    },
    {
      "id": "COR-L3-07",
      "title": "user.dir-based projectRoot is fragile — all 17 tests silently fail when run from IDE or non-root module",
      "severity": "medium",
      "category": "platform",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:10",
      "description": "private val projectRoot = Paths.get(System.getProperty(\"user.dir\")) resolves to the JVM's current working directory at test-class-load time. Gradle sets user.dir to the project root for test tasks, making this work in CI and CLI runs. But IntelliJ IDEA sets user.dir to the module root (which may differ in a multi-module build), and other test runners may set it to the test output directory or the user's home directory. When user.dir is wrong, decisionDoc resolves to a non-existent path. The exists property returns false. Every test that accesses decisionDoc.text throws 'Decision document not found at <wrong path>'. All 17 tests fail with identical error messages that are indistinguishable from the document genuinely being absent. There is no assertion verifying that projectRoot is the expected root. This is COR-07 from loop-1, still present and unfixed.",
      "evidence": "private val projectRoot = Paths.get(System.getProperty(\"user.dir\")) — no validation.\nNo assertion that projectRoot.resolve(\"build.gradle.kts\").exists() or similar anchor check.\nIntelliJ test runner default: Sets working directory to the module's content root, not necessarily the Gradle project root.",
      "scenario": "Developer opens the project in IntelliJ and runs SpikeShader2DecisionSpec directly. IDEA sets user.dir to the root module directory, which matches the Gradle project root in this single-module project. All 17 tests pass. Developer adds a submodule and moves the spike tests into it. Now IDEA sets user.dir to the submodule root. projectRoot resolves to the submodule directory. The decision document is not there. All 17 tests fail with 'Decision document not found'. The failure is silent — no indication that the root cause is the working directory, not a missing file."
    }
  ]
}
