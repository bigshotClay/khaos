{
  "reviewer": "paleontologist",
  "loop": 2,
  "scope": "loop-2 changes only: LazyThreadSafetyMode.NONE in SpikeDecisionDocument.kt, TC-16 and TC-17 in SpikeShader2DecisionSpec.kt, stale test plan intro count",
  "findings": [
    {
      "id": "PAL-L2-01",
      "title": "LazyThreadSafetyMode.NONE on file-scoped singleton is a latent race if Kotest parallelism is ever enabled",
      "severity": "low",
      "category": "coupling",
      "location": "src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:11",
      "description": "NONE was the right fix for the exception-caching problem — it allows the initializer to re-run on each access when the file is absent. For the happy path (file exists, decisionDoc succeeds once), NONE also means the value is never published safely across threads. Today Kotest runs specs sequentially and the singleton's first successful read is uncontested, so this is fine. The trap is: Kotest's coroutine test dispatcher can run 'should' blocks concurrently with config like 'concurrency = N'. If that is ever enabled — even accidentally via a global Kotest config added to unblock a future slow test — two coroutines can race to initialise the lazy. With NONE there is no synchronisation; both can enter the initialiser simultaneously and read the file twice, or one can see a half-constructed String. This is a hobby project and single-threaded test execution is the default, so the risk is low. But the mode name 'NONE' implies a conscious tradeoff that a future maintainer is unlikely to remember.",
      "evidence": "val text: String by lazy(LazyThreadSafetyMode.NONE) { ... } at line 11; decisionDoc declared as file-scope singleton at SpikeShader2DecisionSpec.kt:11",
      "future_cost": "If Kotest concurrency config is added (e.g. to speed up a slow test suite in spike-5+), TC-2 through TC-15 start flaking nondeterministically. Diagnosis is 2-4 hours because the failure is intermittent and the mode name provides no hint."
    },
    {
      "id": "PAL-L2-02",
      "title": "TC-17 tests the lazy retry contract but NONE mode silently breaks if check() is ever replaced with require()",
      "severity": "low",
      "category": "test-debt",
      "location": "src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:12, src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:252-267",
      "description": "TC-17 correctly verifies that the second access on an absent document re-throws rather than returning a cached exception or a cached stale value. The test depends on a specific interaction: NONE mode does not cache exception state, so the initializer lambda runs again, check(exists) re-evaluates path.exists(), the file is still absent, and check throws again. This contract holds only as long as (1) the mode stays NONE, and (2) the initializer uses check/require rather than storing state. If a future edit changes 'check' to 'if (!exists) throw IllegalStateException(...)' and stores null into a backing field, the contract breaks silently — the second call returns the null-initialised value rather than throwing. TC-17 would catch that only if the implementation literally returned a non-throwing result. The test is correct for the current code. The debt is that there is no comment in SpikeDecisionDocument.kt explaining *why* NONE is used and what contract it enables, so the connection between the implementation choice and TC-17 is invisible.",
      "evidence": "No comment in SpikeDecisionDocument.kt explaining LazyThreadSafetyMode.NONE selection; TC-17 comment says 'no lazy exception caching cascade' but the impl file is silent",
      "future_cost": "A refactor that switches check() to a try/catch with a null return silently breaks TC-17's second-access assertion; without a comment the author won't know NONE was load-bearing."
    },
    {
      "id": "PAL-L2-03",
      "title": "TC-16 four-branch OR with two Boolean variables and shouldBe true — failure message names no branch",
      "severity": "medium",
      "category": "maintainability",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:240-248",
      "description": "TC-16 decomposes the assertion into hasFastFail (two alternatives) and hasDeferNote (two alternatives) then tests (hasFastFail || hasDeferNote) shouldBe true. When this assertion fails, Kotest reports 'Expected: true, got: false' inside the withClue message — it does not name which of the four strings was searched or which variable was false. This is worse than the OR-chain pattern in existing TCs (flagged as PAL-02) because the indirection through named Boolean variables looks more readable but loses even more diagnostic context. If the document author writes 'deferred to implementation' (singular noun, not the tested phrase 'error handling is deferred') the test fails with zero indication of which substring was the target. The withClue message says 'must state fail-fast behavior or defer to implementation' which partially compensates, but the clue was written for humans, not for the assertion mechanics.",
      "evidence": "val hasFastFail = text.contains('Fail-fast', ignoreCase = true) || text.contains('fail fast', ignoreCase = true); val hasDeferNote = text.contains('implementation concern', ignoreCase = true) || text.contains('error handling is deferred', ignoreCase = true); (hasFastFail || hasDeferNote) shouldBe true",
      "future_cost": "The decision document for spike-3 uses 'deferred error handling' (adjective-first) — fails TC-16, zero clue which of four strings it needed. ~10 min to re-read the test and the doc to find the mismatch."
    },
    {
      "id": "PAL-L2-04",
      "title": "TC-16 acceptable-alternatives set is underdetermined — 'implementation concern' is too broad",
      "severity": "low",
      "category": "test-debt",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:245",
      "description": "The hasDeferNote branch accepts 'implementation concern' (ignoreCase). This phrase is generic enough to appear in any architectural discussion — 'this is an implementation concern not a design concern', 'caching is an implementation concern'. A decision document that mentions 'implementation concern' in an unrelated context would satisfy TC-16 even if it said nothing about parseReflectionJson error handling. The test was added to prevent the document being silent on error handling, but the weakest alternative branch defeats that intent. At hobby scale this is a minor annoyance, not a blocker — the decision doc currently has the explicit 'Fail-fast' phrase in the parseReflectionJson stub comment, so TC-16 passes on the strong branch.",
      "evidence": "text.contains('implementation concern', ignoreCase = true) — the decision doc contains 'Fail-fast: validate required fields' in the code block, so the test passes on hasFastFail not hasDeferNote. The weak branch is latent.",
      "future_cost": "A revised decision doc that removes the Fail-fast comment but mentions 'This is an implementation concern' somewhere in prose now passes TC-16 falsely, giving the false impression that error handling is stated when it is not."
    },
    {
      "id": "PAL-L2-05",
      "title": "Stale test-plan intro count (says 15, table shows 17) — minor but it will mislead the next loop reviewer",
      "severity": "info",
      "category": "maintainability",
      "location": "test plan intro (referenced in PR #36 description)",
      "description": "The test plan introduction states '15 test cases organized by layer: 5 Acceptance, 7 Design, 3 Failure'. The summary table correctly lists TC-1 through TC-17, 17 cases. Loop 2 added TC-16 and TC-17 without updating the prose count. At hobby scale this is a cosmetic issue — the table is authoritative and readable. The debt is that the next reviewer (or the agent in loop-3) reading the intro will trust '15' as the ground truth, scan for 15 cases, and be confused when the summary table disagrees. It also means the layer breakdown is wrong: TC-16 is a Design test (error handling stated in document) and TC-17 is a Failure test (infrastructure behavior), so the actual breakdown is 5 Acceptance, 8 Design, 4 Failure.",
      "evidence": "PR #36 description: 'Test plan intro still says 15 test cases organized by layer: 5 Acceptance, 7 Design, 3 Failure even though the summary table correctly shows 17'",
      "future_cost": "Loop-3 reviewer reads '15 cases' and flags TC-16/TC-17 as undocumented additions, wasting one review cycle on a counting discrepancy."
    }
  ]
}
