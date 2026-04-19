{
  "reviewer": "prosecutor",
  "review_loop": "2 (2026-04-19 pass)",
  "date": "2026-04-19",
  "summary": {
    "loop2_findings_resolved": ["PRO-L2-03 (factually wrong — retracted)", "PRO-L2-07 (issue body updated — confirmed)"],
    "loop2_findings_carrying_forward": ["PRO-L2-01", "PRO-L2-02", "PRO-L2-04", "PRO-L2-05", "PRO-L2-06", "PRO-L2-08"],
    "new_findings": ["PRO-L3-01", "PRO-L3-02", "PRO-L3-03", "PRO-L3-04"]
  },
  "findings": [
    {
      "id": "PRO-L3-01",
      "title": "PRO-L2-03 retracted: lazy(NONE) does NOT cache exceptions — TC-17 is correct",
      "severity": "critical",
      "category": "ac-gap",
      "location": "src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:11 / planning/reviews/issue-2-prosecutor-loop2-2026-04-18.md:PRO-L2-03",
      "description": "Loop 2 finding PRO-L2-03 claimed: 'lazy(LazyThreadSafetyMode.NONE) caches exceptions identically to lazy(SYNCHRONIZED) — initializer does not re-execute after throw.' This is factually incorrect. Kotlin stdlib source (kotlin-stdlib-2.0.20, commonMain/kotlin/util/Lazy.kt) confirms that NONE maps to UnsafeLazyImpl. In UnsafeLazyImpl, the initializer assignment is `_value = initializer!!()`. If the initializer throws, the assignment never completes and `_value` remains UNINITIALIZED_VALUE. The initializer reference is NOT nulled out (that only happens on success). On second access, `_value === UNINITIALIZED_VALUE` is still true, so the initializer executes again. By contrast, SynchronizedLazyImpl (SYNCHRONIZED mode) also retries on throw — confirmed by its KDoc: 'If the initialization of a value throws an exception, it will attempt to reinitialize the value at next access.' Neither mode caches exceptions in the Kotlin JVM stdlib as of 2.0.20. The TC-17 implementation is correct: both accesses to absentDoc.text throw 'Decision document not found' via genuine re-execution, not exception caching. The test title 'second access retries cleanly' is accurate. PRO-L2-03 must be retracted.",
      "spec_reference": "TC-17: 'Second access must also throw with \"Decision document not found\" — no lazy exception caching cascade'",
      "evidence": "Kotlin stdlib UnsafeLazyImpl (lazy(NONE)): `if (_value === UNINITIALIZED_VALUE) { _value = initializer!!(); initializer = null }`. When initializer throws: `_value = [throws]` — assignment never completes, `_value` stays UNINITIALIZED_VALUE, `initializer` stays non-null. Second call: condition true again, initializer re-executes, check(exists) throws again with same message. TC-17 test lines 259-265: first and second accesses both throw 'Decision document not found'. Test passes via genuine retry, not cache re-throw. PRO-L2-03 claim was wrong."
    },
    {
      "id": "PRO-L3-02",
      "title": "PRO-L2-07 resolved: issue #17 body is edited and contains spike findings",
      "severity": "info",
      "category": "ac-gap",
      "location": "general",
      "description": "Loop 2 finding PRO-L2-07 (critical) stated AC item 5 was unmet because issue #17 body had not been edited. This has been resolved. Running `gh issue view 17 --repo bigshotClay/khaos --json body --jq '.body'` confirms the body now contains Gradle task implementation hints including 'Gradle @CacheableTask', '@InputFiles', '@SkipWhenEmpty', '@PathSensitive', the full four-stage pipeline, `commonMain` wiring instructions, `kotlinx.serialization`, determinism invariant, and fail-fast note. 'Gradle' appears 4 times in the body. TC-5 will pass when executed with GH_TOKEN present (verified manually). AC item 5 is satisfied.",
      "spec_reference": "Issue #2 AC item 5: 'SHADER-2 issue updated with implementation hints from findings'",
      "evidence": "gh issue view 17 body contains: '## Spike Finding: Use Gradle @CacheableTask (not KSP)', 'compileShaders → reflectShaders (spirv-cross --reflect) → generateShaderBindings → compileKotlin', 'kotlin.sourceSets.named(\"commonMain\")', 'Deterministic: same reflection JSON → same Kotlin source → same binary', 'Fail-fast: malformed JSON throws with shader name and missing field named in the error'. grep -c 'Gradle' = 4."
    },
    {
      "id": "PRO-L3-03",
      "title": "PR #36 description is stale: claims 15 tests and comment posted, not 17 tests and body edited",
      "severity": "high",
      "category": "ac-gap",
      "location": "GitHub PR #36 body",
      "description": "The PR #36 description has three inaccuracies introduced by the loop 2 fix commit (214cb25) being pushed to the same branch without updating the PR description: (1) 'Summary' bullet says '15 Kotest tests' — actual count is 17. (2) Summary bullet says 'SHADER-2 (#17) commented with implementation hints' — the actual fix was a body edit via `gh issue edit`, not a comment. (3) The TC-5 section says 'Verified: https://github.com/bigshotClay/khaos/issues/17#issuecomment-4274405449' and the test plan row says 'TC-5 GitHub check passes when GH_TOKEN is present (comment added to #17)' — both reference a COMMENT, but TC-5 spec requires and tests the issue BODY (.body field). The TC-5 skip message in the test says 'Skipping GitHub check — no GH_TOKEN available. Verified in PR description.' A reviewer following this skip message to the PR description finds the wrong artifact (comment link, not body edit confirmation). The Failure row in the PR table also says '3' instead of '5'.",
      "spec_reference": "TC-5: 'A comment does not satisfy this — the body itself must be edited via gh issue edit 17 --body \"...\"'; AC item 5: SHADER-2 issue updated",
      "evidence": "PR #36 body: 'SHADER-2 (#17) commented with implementation hints (Gradle task approach, not KSP)'. TC-5 section: 'Verified: ...#issuecomment-4274405449'. Table: '15 Kotest tests', 'Failure | 3'. Fix commit 214cb25 message: 'gh issue edit 17 body updated with Gradle task spike findings (done via CLI)'. Actual test count in SpikeShader2DecisionSpec.kt: grep -c 'should(' = 17."
    },
    {
      "id": "PRO-L3-04",
      "title": "AC-2 partial gap: spike question Q2 (KSP architecture) has no dedicated answer in decision document",
      "severity": "medium",
      "category": "ac-gap",
      "location": "_bmad-output/planning-artifacts/designs/spike-shader-2-decision.md:sections 1-5",
      "description": "Issue #2 specifies five questions. Question 2 is: 'What is the right KSP processor architecture for this use case?' The decision document contains sections numbered 1 through 5, but the numbering does not match the issue questions. Doc section 2 answers 'Can KSP generate @JvmInline value class or sealed hierarchies?' (issue Q3). Doc section 3 answers 'Is KSP the right tool?' (issue Q4). Issue Q2 'What is the right KSP architecture?' has no dedicated section or explicit acknowledgment. The Verdict rejects KSP entirely, which implicitly makes Q2 moot, but the document does not state this explicitly (e.g., 'Q2 (KSP architecture) is moot: KSP is rejected, no architecture is needed'). TC-2's withClue labels compound this: clue 'Q2 evidence: KSP generated type support must address @JvmInline' and 'Q2 evidence: known IDE regression must cite KSP #1351' map issue Q3 content to a Q2 label. The assertions pass because the content exists, but a reviewer reading the clue labels cannot map them back to the correct issue questions. AC-2 ('All five questions answered with evidence') is borderline satisfied: the answer to Q2 is implied by the rejection, but is not explicit.",
      "spec_reference": "Issue #2 AC-2: 'All five questions above answered with evidence'; Issue Q2: 'What is the right KSP processor architecture for this use case?'",
      "evidence": "Decision doc sections: '1. Can a KSP processor consume external files?' (= issue Q1), '2. Can KSP generate @JvmInline value class or sealed hierarchies?' (= issue Q3), '3. Is KSP the right tool?' (= issue Q4), '4. Does the processor need to run in the same Gradle task graph?' (= issue Q5), '5. Input/output contract' (= AC item 4). Issue Q2 is not a numbered section. TC-2 clue 'Q2 evidence: KSP generated type support must address @JvmInline' — this is Q3 content mislabeled as Q2. TC-2 clue 'Q3 evidence: Gradle CacheableTask must be named as the right tool' — this is Q4 content labeled Q3."
    },
    {
      "id": "PRO-L3-05",
      "title": "CARRY: TC-5 readText() blocks before waitFor() timeout — CI deadlock protection not effective for stdout hang",
      "severity": "high",
      "category": "design-deviation",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:97-98",
      "description": "Carried from PRO-L2-01. Not addressed in the loop 2 fix commit. readText() at line 97 consumes the gh subprocess stdout to EOF synchronously with no timeout. waitFor(30, TimeUnit.SECONDS) at line 98 is only reached after readText() returns. If gh hangs before writing EOF (network stall, DNS timeout, credential prompt), readText() blocks the JVM thread indefinitely. The 30-second timeout never fires. The spec requires the timeout 'to prevent CI deadlock'. With redirectErrorStream(true), stderr is merged and will not cause a separate deadlock, but the stdout blockage remains. The correct fix is to either (a) use a Future/thread to read stdout with a deadline, or (b) read stdout after waitFor() returns by using a bounded read approach. As written, the protection is illusory for this failure mode.",
      "spec_reference": "TC-5: 'The gh subprocess implementing this check must use ... a timeout on waitFor(30, TimeUnit.SECONDS) ... to prevent CI deadlock'",
      "evidence": "SpikeShader2DecisionSpec.kt line 97: `val body = result.inputStream.bufferedReader().readText()` — synchronous, no deadline. Line 98: `val exited = result.waitFor(30, TimeUnit.SECONDS)` — reached only after readText() completes. If gh stdout pipe stays open indefinitely, readText() never returns."
    },
    {
      "id": "PRO-L3-06",
      "title": "CARRY: TC-5 body variable populated from potentially corrupt content before exit code guard",
      "severity": "medium",
      "category": "design-deviation",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:97-103",
      "description": "Carried from PRO-L2-02. Not addressed in the loop 2 fix commit. body is assigned at line 97 from readText() before any exit code check. When gh exits non-zero, body contains error text (merged via redirectErrorStream(true)). The exit code check at line 100 fires before the body assertion at line 102, so Kotest will abort on exit-code failure, preventing the body assertion from running with corrupt content. However, the spec states 'exit code must be checked before asserting on body content' — the body variable is unconditionally populated from potentially corrupt/error output before the guard runs. The implementation matches the spec's observable behavior (Kotest aborts on exit failure) but deviates from the explicit spec ordering intent.",
      "spec_reference": "TC-5: 'gh subprocess exits non-zero — exit code must be checked before asserting on body content'",
      "evidence": "Line 97: body assigned unconditionally. Line 99-100: exit code check. Line 101-103: body assertion. If gh fails, body contains error output but exit code guard at line 100 aborts the test before body assertion runs. Spec intent not violated in practice due to Kotest's throw-on-failure, but the guard does not prevent body variable pollution."
    },
    {
      "id": "PRO-L3-07",
      "title": "CARRY: TC-16 assertion accepts 'Fail-fast' anywhere in document — code comment outside intended context satisfies it",
      "severity": "medium",
      "category": "test-gap",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:241-248",
      "description": "Carried from PRO-L2-04. The TC-16 assertion uses text.contains('Fail-fast', ignoreCase = true). The document contains '// Fail-fast: validate required fields ...' inside the parseReflectionJson code block at line 196. This satisfies the assertion correctly. However, text.contains() scans the entire document: any prose occurrence of 'fail-fast' or 'Fail-fast' anywhere — unrelated to parseReflectionJson — would also satisfy it. The assertion does not verify that the fail-fast note is in the context of the skeleton code or the error-handling description. In the current document, the placement is correct, but the test provides no location guarantee.",
      "spec_reference": "TC-16: 'Document must not be silent on error handling — must state fail-fast behavior or defer to implementation'",
      "evidence": "SpikeShader2DecisionSpec.kt line 243: `text.contains(\"Fail-fast\", ignoreCase = true)`. Document line 196: `// Fail-fast: validate required fields (entryPoints, inputs, ubos, push_constants, textures);` inside fenced code block. Passes. Would also pass if 'Fail-fast' appeared in an unrelated prose sentence."
    },
    {
      "id": "PRO-L3-08",
      "title": "CARRY: Test plan header 'Test count: 15' and intro sentence are stale — actual count is 17",
      "severity": "low",
      "category": "test-gap",
      "location": "_bmad-output/test-artifacts/issue-2-plan.md:9,15",
      "description": "Carried from PRO-L2-05. The test plan metadata table at line 9 reads '| Test count | 15 (5 Acceptance, 7 Design, 3 Failure) |' and the intro sentence at line 15 reads '15 test cases organized by layer: 5 Acceptance, 7 Design, 3 Failure.' TC-16 and TC-17 were added in loop 2. The coverage summary table at the bottom correctly shows 17 total and 5 Failure. The header is stale.",
      "spec_reference": "Test plan metadata vs. coverage summary: '15' vs '17'.",
      "evidence": "Line 9: '| Test count | 15 (5 Acceptance, 7 Design, 3 Failure) |'. Line 15: '15 test cases organized by layer: 5 Acceptance, 7 Design, 3 Failure.' Bottom table: '| Total | 17 | (+2 added, TC-13 superseded by TC-16) |'. grep -c 'should(' SpikeShader2DecisionSpec.kt = 17."
    },
    {
      "id": "PRO-L3-09",
      "title": "CARRY: TC-13 marked SUPERSEDED in test plan but executes as a live test with no annotation",
      "severity": "medium",
      "category": "test-gap",
      "location": "_bmad-output/test-artifacts/issue-2-plan.md:133 / src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:199-213",
      "description": "Carried from PRO-L2-06. TC-13 is labeled '[SUPERSEDED by TC-16]' in the test plan. TC-13 still runs as a live, unannotated test in SpikeShader2DecisionSpec.kt at lines 199-213. The test plan's Coverage Summary counts TC-13 in the Failure layer (5 total), so the supersession is in name only — TC-13 contributes to the passing count. TC-13 and TC-16 have overlapping but distinct assertions: TC-13 checks field names and parseReflectionJson presence; TC-16 checks that error handling approach is stated. Running both is not harmful, but calling TC-13 superseded while running it creates audit ambiguity.",
      "spec_reference": "Test plan TC-13 annotation: '[SUPERSEDED by TC-16]'. Coverage Summary: 'TC-13 superseded by TC-16'.",
      "evidence": "SpikeShader2DecisionSpec.kt lines 199-213: TC-13 present and active, no @Ignore or comment. Test plan line 133: 'TC-13: Input JSON schema names required fields and parsing stub defined [SUPERSEDED by TC-16]'. Both tests contribute to the 17-test total that the plan documents."
    },
    {
      "id": "PRO-L3-10",
      "title": "CARRY: committedToGit() has no subprocess timeout — TC-1 can deadlock in CI",
      "severity": "low",
      "category": "test-gap",
      "location": "src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:22",
      "description": "Carried from PRO-L2-08. committedToGit() calls result.waitFor() with no timeout. TC-5 was updated to use waitFor(30, TimeUnit.SECONDS), but the same pattern was not applied to committedToGit(). If the git subprocess hangs (locked index, NFS timeout, broken git config), TC-1 deadlocks. The fix is result.waitFor(30, TimeUnit.SECONDS) consistent with TC-5.",
      "spec_reference": "TC-1 relies on committedToGit(). TC-5 spec: 'a timeout on waitFor(30, TimeUnit.SECONDS) to prevent CI deadlock'. Pattern not applied consistently.",
      "evidence": "SpikeDecisionDocument.kt line 22: `result.waitFor()` — no timeout, no TimeUnit. SpikeShader2DecisionSpec.kt line 98: `result.waitFor(30, TimeUnit.SECONDS)` — timeout present. Inconsistent subprocess safety."
    }
  ]
}
