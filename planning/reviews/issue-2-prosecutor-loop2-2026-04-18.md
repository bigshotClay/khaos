{
  "reviewer": "prosecutor",
  "review_loop": 2,
  "date": "2026-04-18",
  "findings": [
    {
      "id": "PRO-L2-01",
      "title": "TC-5 timeout is ineffective: readText() blocks before waitFor() is reached",
      "severity": "high",
      "category": "design-deviation",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:97-98",
      "description": "The TC-5 spec states: 'a timeout on waitFor(30, TimeUnit.SECONDS) to prevent CI deadlock.' The implementation calls result.inputStream.bufferedReader().readText() on line 97 BEFORE the waitFor(30, TimeUnit.SECONDS) call on line 98. readText() drains stdout to completion synchronously — it blocks the JVM thread until the gh process closes its stdout pipe, with no timeout. The 30-second waitFor timeout only governs the period after stdout has already been fully consumed. If gh stalls indefinitely before writing EOF to its stdout (e.g., waiting for a credential prompt, network hang, or DNS timeout), readText() hangs forever. The timeout is never reached. The spec requirement 'prevent CI deadlock' is not met for this failure mode. With redirectErrorStream(true) in place, there is no stderr deadlock, but the stdout-blocking hang remains unguarded.",
      "spec_reference": "TC-5: 'The gh subprocess implementing this check must use ... a timeout on waitFor(30, TimeUnit.SECONDS) ... to prevent CI deadlock'",
      "evidence": "Line 97: val body = result.inputStream.bufferedReader().readText() — blocks until EOF with no timeout. Line 98: val exited = result.waitFor(30, TimeUnit.SECONDS) — only reached after readText() returns. If gh hangs at stdout write (DNS stall, network hang), readText() never returns and the 30-second timeout never fires."
    },
    {
      "id": "PRO-L2-02",
      "title": "TC-5 exit code checked after body assertion would already have failed — ordering defeats the guard",
      "severity": "medium",
      "category": "design-deviation",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:99-103",
      "description": "The TC-5 spec states: 'gh subprocess exits non-zero — exit code must be checked before asserting on body content.' The implementation checks the exit code on lines 99-100 but the withClue blocks execute in source order. When gh exits non-zero, body will be an error message from stderr (merged via redirectErrorStream(true)) or an empty string. The exit code assertion fires at line 100, but by the time the test fails there, the body variable already contains error output — the body shouldContain 'Gradle' on line 101 would fire immediately afterward with a confusing failure message ('Expected \"error: ...\" to contain \"Gradle\"') unless Kotest's shouldBe assertion throws and aborts the test. Kotest does abort on first failure inside a should block, so the exit code check on line 100 does succeed as a guard IF gh exits non-zero. However, the spec is explicit that exit code must be checked before asserting on body content — the current code evaluates body = readText() before any exit check, meaning the body variable is bound to potentially corrupt content before the guard runs. This is a sequencing deviation from spec intent.",
      "spec_reference": "TC-5: 'gh subprocess exits non-zero — exit code must be checked before asserting on body content'",
      "evidence": "body is bound at line 97 from readText() before any exit check. The exit code withClue at line 100 runs before the body assertion at line 102, so Kotest will abort on exit-code failure — but the spec says 'exit code must be checked before asserting on body content', and the body variable is populated unconditionally from potentially corrupt/error output before the check."
    },
    {
      "id": "PRO-L2-03",
      "title": "TC-17: LazyThreadSafetyMode.NONE does not re-execute the lambda on second access after exception",
      "severity": "critical",
      "category": "design-deviation",
      "location": "src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:11",
      "description": "TC-17 requires: 'Second access must also throw with \"Decision document not found\" — no lazy exception caching cascade.' The implementation uses lazy(LazyThreadSafetyMode.NONE). The Kotlin documentation for LazyThreadSafetyMode.NONE states that in this mode, the initializer is NOT thread-safe but the exception-caching behavior is IDENTICAL to the default SYNCHRONIZED mode: if the initializer throws, the exception IS cached and re-thrown on subsequent accesses. LazyThreadSafetyMode.NONE only removes synchronization locks — it does not change the exception-caching contract. The TC-17 test accesses absentDoc.text twice and expects both to throw 'Decision document not found'. With lazy(NONE), the first access throws and caches the exception; the second access re-throws the SAME cached exception. This means the second access will still contain 'Decision document not found' in its message (because the check() call caches that exact exception), so the test MAY pass — but not because of the retry behavior the spec intends. The spec says 'retries cleanly', implying the file is re-checked on second access (for the use case where the document appears on disk between the two accesses). With any lazy mode, the initializer never runs a second time after a first-access exception. The TC-17 implementation note says 'Use LazyThreadSafetyMode.NONE' — this satisfies the letter of the note but not the spirit of the requirement ('second access retries cleanly').",
      "spec_reference": "TC-17: 'Second access must also throw with \"Decision document not found\" — no lazy exception caching cascade'; Implementation note: 'Use LazyThreadSafetyMode.NONE'",
      "evidence": "SpikeDecisionDocument.kt line 11: val text: String by lazy(LazyThreadSafetyMode.NONE). Kotlin lazy(NONE) caches exceptions identically to lazy(SYNCHRONIZED) — initializer does not re-execute after throw. Test at lines 259-265 will pass (same exception re-thrown), but via caching, not retry. 'Retries cleanly' is not implemented. If the file appears between first and second access, second access still throws from cache."
    },
    {
      "id": "PRO-L2-04",
      "title": "TC-16 assertion satisfies spec but document skeleton comment is not inside a code block — hasCodeBlock would not find it",
      "severity": "medium",
      "category": "test-gap",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:241-248 / _bmad-output/planning-artifacts/designs/spike-shader-2-decision.md:195-198",
      "description": "TC-16 asserts hasFastFail (text.contains('Fail-fast', ignoreCase = true)) OR hasDeferNote. The decision document's parseReflectionJson stub includes the comment '// Fail-fast: validate required fields...' at line 196 of spike-shader-2-decision.md. This comment is inside a fenced code block, so text.contains('Fail-fast') will find it via plain text search and TC-16 passes. This is spec-compliant. However, the TC-16 assertion uses text.contains() not hasCodeBlock() — it would also pass if 'Fail-fast' appeared anywhere in prose outside a code block. This is an over-broad assertion that happens to be satisfied by the right evidence. No finding on correctness, but the test does not verify that the fail-fast note is in a code/skeleton context vs. ambient prose.",
      "spec_reference": "TC-16: 'Either the skeleton includes a note like \"validate required fields; throw with shader name on missing field\" OR a section note says \"detailed error handling is an implementation concern beyond the spike scope.\"'",
      "evidence": "spike-shader-2-decision.md lines 195-198 show the Fail-fast comment inside the code block. text.contains() search covers entire document text including code blocks, so hasFastFail = true. Test passes. Assertion is weaker than spec intent (code block placement not verified)."
    },
    {
      "id": "PRO-L2-05",
      "title": "Test plan header count stale — states 15 test cases, 3 Failure; actual total is 17 with 5 Failure",
      "severity": "low",
      "category": "test-gap",
      "location": "_bmad-output/test-artifacts/issue-2-plan.md:10,16",
      "description": "The test plan metadata table (line 10) and the intro sentence (line 16) both state '15 test cases organized by layer: 5 Acceptance, 7 Design, 3 Failure.' TC-16 and TC-17 were added in loop 2 (documented at line 187: 'Total: 17, +2 added, TC-13 superseded by TC-16'). The coverage summary table at the bottom correctly shows 17 total and 5 Failure. The header is stale and contradicts the summary. Any automated tooling that parses the header count will misreport test coverage.",
      "spec_reference": "Test plan: 'Test count: 15' vs. Coverage Summary: 'Total: 17'. TC-16 and TC-17 spec additions confirmed in the plan.",
      "evidence": "Line 10: '| Test count | 15 (5 Acceptance, 7 Design, 3 Failure) |'. Line 16: '15 test cases organized by layer: 5 Acceptance, 7 Design, 3 Failure.' Line 187: '| Total | 17 | (+2 added, TC-13 superseded by TC-16) |'. Actual test count in spec: grep 'should(' SpikeShader2DecisionSpec.kt = 17."
    },
    {
      "id": "PRO-L2-06",
      "title": "TC-13 marked SUPERSEDED in test plan but the test still exists and runs in the spec",
      "severity": "medium",
      "category": "test-gap",
      "location": "_bmad-output/test-artifacts/issue-2-plan.md:133 / src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:199-213",
      "description": "TC-13 is labeled '[SUPERSEDED by TC-16]' in the test plan at line 133. The test plan's intent is that TC-16 replaces TC-13's error-handling verification. However, TC-13 continues to exist as a live, passing test in SpikeShader2DecisionSpec.kt at lines 199-213 with no annotation indicating it is superseded. TC-13 and TC-16 now both execute: TC-13 checks that parseReflectionJson and required field names appear in the document; TC-16 checks that error handling approach is stated. This is not necessarily wrong (TC-13's field-name assertions are distinct from TC-16's error-handling assertion), but the test plan says TC-13 is superseded, implying it should either be removed or annotated. Running a 'superseded' test creates ambiguity about what the passing count means.",
      "spec_reference": "Test plan TC-13: '[SUPERSEDED by TC-16]'. Coverage Summary: 'TC-13 superseded by TC-16'.",
      "evidence": "SpikeShader2DecisionSpec.kt lines 199-213: TC-13 test is live and unmarked. Total test count = 17 in both plan and spec, but plan counts TC-13 as one of the 5 Failure cases while also noting it is superseded by TC-16. Both tests contribute to the 17-count, so the supersession is in name only."
    },
    {
      "id": "PRO-L2-07",
      "title": "AC item 5 remains unmet — issue #17 body not updated, prior loop 1 finding not resolved",
      "severity": "critical",
      "category": "ac-gap",
      "location": "general",
      "description": "Loop 1 finding PRO-03 identified that issue #2 AC item 5 ('SHADER-2 issue updated with implementation hints from findings') is not met because spike findings were posted as a comment on issue #17 instead of editing the issue body. The loop 2 changes (TC-16, TC-17, decision doc skeleton update, redirectErrorStream/scoped env in TC-5) do not include updating the issue #17 body. AC item 5 remains open. TC-5's assertion (body shouldContain 'Gradle') will still fail when run with GH_TOKEN, as there is no evidence in the loop 2 changes that `gh issue edit 17 --body '...'` was executed.",
      "spec_reference": "Issue #2 AC item 5: 'SHADER-2 issue updated with implementation hints from findings'; TC-5 spec: 'A comment does not satisfy this — the body itself must be edited via gh issue edit 17 --body \"...\"'",
      "evidence": "Loop 1 PRO-03 filed as critical. Loop 2 changes: TC-16 added, TC-17 added, skeleton comment added, TC-5 subprocess scoped. No gh issue edit command in any changed file. Issue #17 body status unchanged from loop 1."
    },
    {
      "id": "PRO-L2-08",
      "title": "redirectErrorStream(true) + scoped env: AC satisfied but waitFor in committedToGit() remains untimed",
      "severity": "low",
      "category": "test-gap",
      "location": "src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:22",
      "description": "The TC-5 spec required redirectErrorStream(true) and scoped environment for the gh subprocess. These are correctly implemented in loop 2 (lines 94-95 of SpikeShader2DecisionSpec.kt). However, the committedToGit() method in SpikeDecisionDocument.kt at line 22 calls result.waitFor() without a timeout — an issue flagged in the loop 1 gauntlet (INF-07, COR-06). This is not a new loop 2 regression, but the loop 2 changes did not address it. The TC-5 subprocess changes should have been applied consistently to the committedToGit() subprocess as well.",
      "spec_reference": "TC-5: 'a timeout on waitFor(30, TimeUnit.SECONDS) ... to prevent CI deadlock'. TC-1 relies on committedToGit() which has no equivalent protection.",
      "evidence": "SpikeDecisionDocument.kt line 22: result.waitFor() — no timeout, no TimeUnit. SpikeShader2DecisionSpec.kt line 98: result.waitFor(30, TimeUnit.SECONDS) — timeout present. Inconsistent application of the subprocess safety pattern."
    }
  ]
}
