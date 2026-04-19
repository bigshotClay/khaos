{
  "reviewer": "paleontologist",
  "findings": [
    {
      "id": "PAL-01",
      "title": "Duplicate string checks across TCs create cascading failures on document rename",
      "severity": "medium",
      "category": "test-debt",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:33,104",
      "description": "'1677' appears in TC-2 and TC-6. 'push_constants' in TC-2, TC-4, and TC-13. 'InputChanges' via hasCodeBlock in TC-7 and TC-14. A document rename triggers cascading red across unrelated TCs.",
      "evidence": "text shouldContain '1677' at lines 33 and 104; shouldContain 'push_constants' at lines 48, 74, 202; hasCodeBlock('InputChanges') at lines 127 and 212",
      "future_cost": "By spike-5, a term rename triggers 3-4 simultaneous TC failures with no clear diagnostic — ~30 min triage per incident"
    },
    {
      "id": "PAL-02",
      "title": "OR-chain boolean assertions mask which branch actually satisfied the check",
      "severity": "medium",
      "category": "test-debt",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:59,65,107,110,137,163,177,188,229",
      "description": "Nine assertions use (text.contains('X') || text.contains('Y')) shouldBe true. When passing, no record of which branch matched. Document can satisfy a weak synonym while the intended signal is absent.",
      "evidence": "TC-3 line 59, TC-6 line 110 four-branch OR, TC-11 lines 163-165, TC-12 line 188",
      "future_cost": "At spike-5, a 1500-word document will contain 'no way' in passing prose, permanently neutralizing TC-6's third assertion"
    },
    {
      "id": "PAL-03",
      "title": "hasSection vs text shouldContain used interchangeably for section headers",
      "severity": "medium",
      "category": "maintainability",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:22,51",
      "description": "TC-1 uses decisionDoc.hasSection('## Verdict'). TC-2 uses text shouldContain '## Sources'. Both check section headers but use different APIs — hasSection exists specifically for this purpose.",
      "evidence": "Line 22: hasSection('## Verdict'). Line 51: text shouldContain '## Sources'",
      "future_cost": "When hasSection is upgraded to assert line-start anchoring, raw text shouldContain calls diverge silently"
    },
    {
      "id": "PAL-04",
      "title": "TC-9 and TC-10 check 'commonMain' via different methods with divergent semantics",
      "severity": "low",
      "category": "test-debt",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:156,166",
      "description": "TC-9 uses text shouldContain 'commonMain' (any prose). TC-10 uses hasCodeBlock('commonMain') (code block only). Intent of both is 'generated types target commonMain' but they test structurally different things with no documentation of the distinction.",
      "evidence": "Line 156: text shouldContain 'commonMain'. Line 166: decisionDoc.hasCodeBlock('commonMain') shouldBe true",
      "future_cost": "Document using inline code span instead of fenced block satisfies TC-9 but fails TC-10"
    },
    {
      "id": "PAL-06",
      "title": "File-scope decisionDoc singleton — shared mutable lazy state across all tests",
      "severity": "low",
      "category": "coupling",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:9-12",
      "description": "decisionDoc is package-scope, lazy, cached forever. Cannot be overridden or reset between test runs in watch-mode Gradle daemon. Impossible to write 'missing file' error tests without a new class.",
      "evidence": "private val decisionDoc = SpikeDecisionDocument(...) at file scope",
      "future_cost": "Adding 'file missing' behavior tests requires class-level refactoring"
    },
    {
      "id": "PAL-07",
      "title": "SpikeDecisionDocument instantiated with hand-copied path per spike — no factory or convention enforcement",
      "severity": "low",
      "category": "scale",
      "location": "src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:7",
      "description": "Both spike specs copy the path pattern manually. No factory method or naming convention helper. Path typo in spike-3 silently creates an always-failing TC-1.",
      "evidence": "SpikeShader1DecisionSpec line 11 and SpikeShader2DecisionSpec line 11 both hand-copy the path with manual name substitution",
      "future_cost": "At spike-5, a path typo fails TC-1 with 'file not found' rather than 'wrong path' — ~15 min diagnosis"
    },
    {
      "id": "PAL-08",
      "title": "JaCoCo on test-only project produces empty XML — misleading 100% coverage claim",
      "severity": "low",
      "category": "complexity",
      "location": "build.gradle.kts:3,20-29",
      "description": "No src/main/kotlin exists. JaCoCo instruments production bytecode. Empty XML produces '100% of zero lines = zero signal'. The completion JSON claims '100.0% coverage', which will confuse tooling when production code is eventually added.",
      "evidence": "No src/main/kotlin directory. issue-1-completion.json: 'coverage_achieved': '100.0%'",
      "future_cost": "When production code added (issue #17), CI baseline immediately regresses from 100% and requires reconfiguration"
    },
    {
      "id": "PAL-09",
      "title": "TC-5 GitHub check silently passes when GH_TOKEN absent — no skip/pending status",
      "severity": "low",
      "category": "test-debt",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:83-98",
      "description": "println + return@should produces a green test with no assertion. Kotest 5.x has assumeTrue / xshould for explicit skip states. Cannot distinguish 'passed' from 'skipped with green' in CI report.",
      "evidence": "if (token == null) { println(...); return@should }",
      "future_cost": "Pattern copied to spike-3/4/5 produces N silently-green GitHub checks across the spike suite"
    }
  ]
}
