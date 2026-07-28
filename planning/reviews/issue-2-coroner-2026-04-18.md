{
  "reviewer": "coroner",
  "findings": [
    {
      "id": "COR-01",
      "title": "path.parent is null for root-level Path — NPE in committedToGit()",
      "severity": "medium",
      "category": "null",
      "location": "src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:18",
      "description": "Path.parent returns null when the path has no parent. Calling .toFile() on null NPEs before the process starts.",
      "evidence": ".directory(path.parent.toFile()) — no null guard",
      "scenario": "SpikeDecisionDocument constructed with a single-component Path"
    },
    {
      "id": "COR-02",
      "title": "lazy text property — exception during init corrupts all subsequent tests sharing the singleton",
      "severity": "high",
      "category": "exception",
      "location": "src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:11-14, SpikeShader2DecisionSpec.kt:10-12",
      "description": "decisionDoc is a file-level singleton. Kotlin's lazy caches and re-throws exceptions on every access. If the document doesn't exist, every test after TC-1 fails with the same cached IllegalStateException, obscuring which tests would pass once the document exists.",
      "evidence": "val text: String by lazy { check(exists) { ... } } — default lazy mode. Singleton declared at file scope.",
      "scenario": "Decision document not yet written mid-sprint"
    },
    {
      "id": "COR-03",
      "title": "TC-5 gh subprocess — redirectErrorStream(false) with no stderr drain causes deadlock",
      "severity": "high",
      "category": "concurrency",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:89-97",
      "description": "stdout is read to completion before waitFor(). If gh writes >64KB to stderr (verbose auth error, rate limit trace), the child blocks on its stderr write while the parent blocks on readText() waiting for stdout — deadlock. No timeout set.",
      "evidence": "redirectErrorStream(false) on line 92, followed by result.inputStream.bufferedReader().readText() with no concurrent stderr drain",
      "scenario": "gh outputs verbose auth failure to stderr. macOS pipe buffer ~65536 bytes. Deadlock hangs Gradle test task until CI job timeout."
    },
    {
      "id": "COR-04",
      "title": "TC-5 gh non-zero exit — empty body silently fails assertion with confusing error",
      "severity": "medium",
      "category": "external",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:93-97",
      "description": "result.waitFor() return value discarded. On gh failure, body is empty string. shouldContain(\"Gradle\") fails with 'Expected \"\" to contain \"Gradle\"' — no indication that the failure was a subprocess error, not a missing update.",
      "evidence": "result.waitFor() return value unused. No exit code check before asserting on body.",
      "scenario": "CI has GH_TOKEN but token lacks repo scope. gh exits 1, body is \"\", test fails with confusing assertion error."
    },
    {
      "id": "COR-05",
      "title": "ProcessBuilder.start() IOException uncaught — git/gh not on PATH produces opaque error",
      "severity": "medium",
      "category": "exception",
      "location": "src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:17-23, SpikeShader2DecisionSpec.kt:89",
      "description": "If git or gh is not on PATH, ProcessBuilder.start() throws IOException. Propagates uncaught, failing tests with raw stack traces instead of meaningful assertion messages.",
      "evidence": "No try/catch around ProcessBuilder(...).start()",
      "scenario": "Docker image with minimal JDK only — git not on PATH. TC-1 throws IOException."
    },
    {
      "id": "COR-06",
      "title": "committedToGit() — waitFor() return discarded; non-zero git exit with non-blank error output returns true",
      "severity": "medium",
      "category": "boundary",
      "location": "src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:22-23",
      "description": "Git error messages ('fatal: not a git repository') are non-blank, so committedToGit() returns true even when git exits non-zero and the file is not committed.",
      "evidence": "return output.isNotBlank() — success depends on byte presence not git exit code",
      "scenario": "_bmad-output is an uninitialized git submodule. git exits 128 with error message. TC-1 passes falsely."
    },
    {
      "id": "COR-07",
      "title": "user.dir may not be project root — all 15 tests silently fail with 'not found'",
      "severity": "medium",
      "category": "platform",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:9",
      "description": "System.getProperty(\"user.dir\") reflects JVM working directory. IDE test runners or multi-module builds may set this to a non-root directory. All 15 tests fail with 'document not found', indistinguishable from the document genuinely missing.",
      "evidence": "private val projectRoot = Paths.get(System.getProperty(\"user.dir\")) — no assertion this resolves to expected root",
      "scenario": "Developer runs spec from IntelliJ. IDEA sets user.dir to module root. All 15 tests fail."
    },
    {
      "id": "COR-08",
      "title": "OR-chain boolean assertions — short-circuit masks which condition actually matched",
      "severity": "low",
      "category": "boundary",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:59,65,107,110,137,163,177,188,229",
      "description": "Nine assertions use (text.contains(\"X\") || text.contains(\"Y\")) shouldBe true. Short-circuit means only the first matching branch executes. The document can satisfy a weak synonym while the intended signal is absent.",
      "evidence": "TC-3 line 59, TC-6 lines 107-112, TC-11 lines 163-165, TC-12 line 188 etc — OR-chain with shouldBe true",
      "scenario": "Document author writes 'not use KSP' but omits explicit 'Do not use KSP' verdict heading. TC-3 passes on weak branch."
    },
    {
      "id": "COR-11",
      "title": "hasCodeBlock regex uses DOT_MATCHES_ALL redundantly — misleads future maintainers",
      "severity": "info",
      "category": "boundary",
      "location": "src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:31",
      "description": "DOT_MATCHES_ALL only affects '.' metacharacter. Pattern uses [\\s\\S] which already handles newlines. The option is a no-op and implies false intent.",
      "evidence": "Regex(\"```[\\\\s\\\\S]*?```\", RegexOption.DOT_MATCHES_ALL) — DOT_MATCHES_ALL has no effect on this pattern",
      "scenario": "Future maintainer removes DOT_MATCHES_ALL when refactoring to use '.*', silently breaks multiline matching"
    }
  ]
}
