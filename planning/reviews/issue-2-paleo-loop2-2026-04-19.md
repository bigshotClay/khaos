{
  "reviewer": "paleontologist",
  "loop": 2,
  "date": "2026-04-19",
  "scope": "Test suite long-term debt: phrase coupling, OR-chain fragility, regex recompilation, hasCodeBlock scope-blindness, TC-5 environment-clear bug, redundant OR branch in TC-3",
  "findings": [
    {
      "id": "PAL-L2-01",
      "title": "hasCodeBlock recompiles regex on every call — 14 calls per test run, no caching",
      "severity": "low",
      "category": "complexity",
      "location": "src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:31",
      "description": "hasCodeBlock() constructs a new Regex object on every invocation with no caching. The spec calls it 14 times per run (15 if TC-10's OR second branch evaluates). Each call allocates a new pattern, compiles it, then calls findAll over 5 code blocks totaling ~4344 chars. At 14 calls that is 14 pattern compilations and 70 block scans to check 13 distinct terms. The method also re-accesses .text on each call (via the lazy), though the lazy handles caching after first read. For a hobby test suite of 17 TCs this is imperceptible. The debt is that the pattern belongs as a class-level val (companion object or top-level) — the current implementation leaks the decision to avoid this as a style matter and future maintainers will copy the pattern when adding spike-3, spike-4 specs, multiplying the cost.",
      "evidence": "fun hasCodeBlock(content: String): Boolean { val codeBlockPattern = Regex(\"```[\\\\s\\\\S]*?```\", RegexOption.DOT_MATCHES_ALL) — fresh Regex on each of 14 calls. grep count: 14 hasCodeBlock invocations in SpikeShader2DecisionSpec.kt.",
      "future_cost": "At spike-5 (5 specs, 14+ calls each) the pattern is copy-pasted into every SpikeNDecisionSpec. A fix then requires editing 5+ files instead of 1."
    },
    {
      "id": "PAL-L2-02",
      "title": "TC-3 OR has a redundant substring branch — 'not use KSP' is always true when 'Do not use KSP' is true",
      "severity": "low",
      "category": "test-debt",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:60",
      "description": "TC-3 assertion: (text.contains(\"Do not use KSP\") || text.contains(\"not use KSP\")). The first branch is a strict substring of the second — 'Do not use KSP' contains 'not use KSP'. The OR branches are not independent alternatives. Branch 1 fails if and only if branch 2 also fails (because 'Do not use KSP' contains 'not use KSP' as a suffix). This means the OR provides zero coverage beyond the weaker branch alone. In practice the test passes on branch 2 regardless of branch 1. Consequence: if the document is reformatted to 'Avoid KSP' or 'KSP is not recommended', both branches fail even though the rejection intent is clear — the OR fails to handle the natural alternatives it appears to be guarding against.",
      "evidence": ">>> s = 'Do not use KSP'; 'not use KSP' in s → True; 'Do not use KSP' in s → True. The second branch is a superset and will never be the exclusive true branch.",
      "future_cost": "Future editor rewrites the Verdict to 'KSP is not recommended' (standard tech-doc phrasing) — both branches fail. The failure message says 'Verdict must explicitly reject KSP' with no hint of what phrasing is accepted, requiring reading both the test and the doc to diagnose."
    },
    {
      "id": "PAL-L2-03",
      "title": "7 OR-chain assertions produce undiagnosable failures — 23 branches with no branch attribution in failure output",
      "severity": "medium",
      "category": "test-debt",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:60,66,113,116,143,169,183,235",
      "description": "Seven inline OR-chain assertions use pattern (text.contains(\"A\") || text.contains(\"B\") || ...) shouldBe true. Total branch count: 2+4+2+4+2+3+2 = 19 branches, plus 4 more through TC-16's Boolean variable indirection. When any of these fails, Kotest reports 'Expected: true, got: false' with the withClue message — but no indication of which branch or substring was searched. The clue text was written to describe intent, not to name the literals being checked. A maintainer diagnosing a failure must read the test source, identify all branches, and manually search the document for each. The OR also creates false confidence: a weak synonym in passing prose can satisfy the assertion while the intended signal is absent. Example: TC-6's 4-branch OR passes on 'no mechanism' in Verdict prose even if 'no supported' is used in a different context.",
      "evidence": "TC-3:60 (2 branches), TC-3:66 (4 branches), TC-6:113 (2 branches), TC-6:116 (4 branches), TC-8:143 (2 branches), TC-10:169 (2 branches via hasCodeBlock), TC-11:183 (3 branches), TC-15:235 (2 branches).",
      "future_cost": "Spike-3 decision document uses 'should not rely on KSP' — fails TC-3's 2-branch OR. Failure message: 'Verdict must explicitly reject KSP: expected true but was false'. Triage time: ~10-15 min per failing OR-chain assertion."
    },
    {
      "id": "PAL-L2-04",
      "title": "hasCodeBlock is scope-blind — TC-9 clue says 'generated output code block' but assertion searches all 5 blocks",
      "severity": "medium",
      "category": "test-debt",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:152-160",
      "description": "TC-9 verifies '@JvmInline value class', 'sealed interface', and 'data class' must appear 'in generated output code block'. hasCodeBlock() scans ALL fenced code blocks in the document, not the specific block where generated Kotlin types are shown. The document has 5 code blocks: pipeline diagram, JSON input example, generated output example (block 3), Gradle task skeleton (block 4), build.gradle.kts registration (block 5). TC-9's assertions pass because block 3 (the correct one) contains all three terms. But if the document is reorganized and these terms appear only in block 4 (the task skeleton — which could plausibly contain a ShaderReflection data class), TC-9 passes despite the generated output example being absent. The assertion proves 'these terms appear somewhere in a code block' not 'the generated output example is shown'.",
      "evidence": "Block analysis: '@JvmInline value class'→block 3 only, 'sealed interface'→block 3 only, 'data class'→block 3 only (currently). If ShaderReflection data class is added to block 4 (task skeleton), 'data class' assertion becomes permanently ambiguous.",
      "future_cost": "Spike-3 doc has a task skeleton (block 2) with 'data class ShaderMeta(...)' for internal use. TC-9's 'data class' assertion passes on block 2 even though the generated output example section is missing. False green persists until a human re-reads the document."
    },
    {
      "id": "PAL-L2-05",
      "title": "8 duplicate term checks across TCs cause cascade failures on document term changes",
      "severity": "medium",
      "category": "coupling",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt",
      "description": "Eight terms are asserted in more than one TC: 'push_constants' (TC-2, TC-4, TC-13), '1677' (TC-2, TC-6), '1351' (TC-2, TC-15), 'CacheableTask' (TC-2, TC-3), 'reflectShaders' (TC-2, TC-8), 'ubos' (TC-4, TC-13), 'InputChanges' via hasCodeBlock (TC-7, TC-14), '@JvmInline value class' via hasCodeBlock (TC-4, TC-9). If 'push_constants' is renamed to 'pushConstants' in a JSON schema update, TC-2, TC-4, and TC-13 fail simultaneously. The failure report lists three separate TC failures with three different withClue messages, none of which say 'push_constants was renamed' — a maintainer sees three unrelated-looking failures that share a single root cause.",
      "evidence": "Python analysis: push_constants: TC-2 TC-4 TC-13 (3x); 1677: TC-2 TC-6 (2x); 1351: TC-2 TC-15 (2x); CacheableTask: TC-2 TC-3 (2x); reflectShaders: TC-2 TC-8 (2x); ubos: TC-4 TC-13 (2x); InputChanges via hasCodeBlock: TC-7 TC-14 (2x); @JvmInline value class via hasCodeBlock: TC-4 TC-9 (2x).",
      "future_cost": "One JSON field rename in the decision document causes 3 simultaneous TC failures. With 17 TCs and no cross-reference comments, triage requires reading all 3 failing TCs to find the shared term. ~20-30 min per incident."
    },
    {
      "id": "PAL-L2-06",
      "title": "TC-5 clears subprocess environment (including PATH) before exec — latent IOException when GH_TOKEN is set",
      "severity": "high",
      "category": "complexity",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:95",
      "description": "TC-5 calls pb.environment().also { it.clear(); it[\"GH_TOKEN\"] = token } to isolate the subprocess environment. This strips ALL environment variables — including PATH — from the subprocess before exec. Java's ProcessBuilder uses execvp(3) for bare command names ('gh'), which resolves the binary by searching PATH. With PATH absent (empty string after clear()), execvp cannot find 'gh' and throws IOException: 'No such file or directory' before any assertion runs. This failure only fires when GH_TOKEN is present (i.e., in CI with the token set), which is exactly the environment where TC-5 is supposed to run. The early-return guard (token == null) masks the bug in local development. SpikeShader1DecisionSpec TC-5b does NOT clear the environment — it uses default inheritance — making the two specs inconsistent in subprocess handling.",
      "evidence": "Line 95: pb.environment().also { it.clear(); it[\"GH_TOKEN\"] = token }. SpikeShader1DecisionSpec.kt line 100-102: ProcessBuilder('gh', ...).redirectErrorStream(false).start() — no environment manipulation.",
      "future_cost": "First CI run with GH_TOKEN configured: TC-5 throws IOException mid-test, not an assertion failure. The error appears as a test execution error rather than a failed assertion, making the CI report harder to parse. The fix (preserve PATH or use absolute path) requires understanding ProcessBuilder's exec semantics."
    },
    {
      "id": "PAL-L2-07",
      "title": "File-scope decisionDoc singleton has no comment explaining the pattern — future spike specs will copy it blindly",
      "severity": "low",
      "category": "maintainability",
      "location": "src/test/kotlin/khaos/spike/SpikeShader2DecisionSpec.kt:10-13",
      "description": "Both SpikeShader1DecisionSpec and SpikeShader2DecisionSpec declare a package-level decisionDoc as a private val with a hand-copied path. Neither file has a comment explaining why the singleton is at file scope (vs. a class-level property or beforeSpec initialization), what the path convention is (_bmad-output/planning-artifacts/designs/spike-N-decision.md), or why LazyThreadSafetyMode.NONE is used. BOND.md notes the pattern ('contracts = document helper class, tests = content assertions') but does not address the instantiation pattern. A developer adding spike-3 will copy the pattern literally, incrementing the spike number, without understanding that the path convention is enforced nowhere and the NONE mode has a specific thread-safety contract. The pattern compounds: 3 files with the same silent assumption, then 4, then 5.",
      "evidence": "SpikeShader1DecisionSpec.kt:10-12 and SpikeShader2DecisionSpec.kt:10-12 are structurally identical with only 'shader-1' vs 'shader-2' differing. No comment in either file or in SpikeDecisionDocument.kt explains the instantiation contract.",
      "future_cost": "Spike-3 author copies the pattern, uses '_bmad-output/designs/spike-3-decision.md' (wrong subdirectory — drops 'planning-artifacts'). TC-1 fails with 'file must exist at ...' — not 'wrong path format'. ~15 min diagnosis."
    },
    {
      "id": "PAL-L2-08",
      "title": "DOT_MATCHES_ALL with [\\s\\S]*? is a redundant regex option — signals misunderstanding that compounds in copies",
      "severity": "info",
      "category": "maintainability",
      "location": "src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:31",
      "description": "The hasCodeBlock regex is Regex(\"```[\\\\s\\\\S]*?```\", RegexOption.DOT_MATCHES_ALL). The DOT_MATCHES_ALL option makes . match newlines — but the pattern uses [\\\\s\\\\S] (which already matches newlines by definition) rather than .. The two options are redundant: [\\\\s\\\\S] works without DOT_MATCHES_ALL; and if . were used instead, DOT_MATCHES_ALL would be required. The current code applies a flag that has no effect on the written pattern. This is not a bug — the regex works correctly — but it signals that the author was uncertain about regex semantics and applied both mechanisms defensively. When this pattern is copied to spike-3/4/5 specs, the same confusion propagates. A reviewer seeing this in spike-5 cannot tell whether DOT_MATCHES_ALL is intentional (in case . is added later) or cargo-culted.",
      "evidence": "Regex(\"```[\\\\s\\\\S]*?```\", RegexOption.DOT_MATCHES_ALL) — DOT_MATCHES_ALL affects only the . metacharacter, which does not appear anywhere in this pattern.",
      "future_cost": "Spike-4 author adds a . to the pattern to match 'any language tag': Regex(\"```.*?```\", RegexOption.DOT_MATCHES_ALL) — now DOT_MATCHES_ALL IS load-bearing but only if newlines appear in the tag. This silent mode change causes confusing test behavior that takes ~30 min to isolate."
    }
  ]
}
