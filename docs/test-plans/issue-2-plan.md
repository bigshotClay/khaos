# Test Plan — Issue #2: SPIKE-SHADER-2
**Validate KSP processor for SPIR-V reflection → typed Kotlin bindings**

| Field | Value |
|---|---|
| Issue | [#2](https://github.com/bigshotClay/khaos/issues/2) |
| Date | 2026-04-18 |
| Author | Sentinel |
| Design input | `_bmad-output/planning-artifacts/designs/spike-shader-2-decision.md` |
| Test count | 18 (5 Acceptance, 7 Design, 6 Failure) |

---

## Test Cases

18 test cases organized by layer: 5 Acceptance, 7 Design, 6 Failure. See subsections below.

## Acceptance Coverage

### TC-1: Decision document committed at the agreed path
**Layer:** Acceptance  
**Verifies:** AC — "Decision document committed to the agreed path"  
**Condition:** Check that `_bmad-output/planning-artifacts/designs/spike-shader-2-decision.md` exists and is non-empty  
**Expected:** File exists, contains a Verdict section, and is committed to version control  
**Implementation constraints:** `committedToGit()` must use `waitFor(30, TimeUnit.SECONDS)` (not bare `waitFor()`). On timeout call `destroyForcibly()` and return `false`. Must check `result.exitValue() == 0` before evaluating `isNotBlank()` — git error output merged via `redirectErrorStream(true)` is non-blank and would otherwise yield a false positive.  
**Edge cases:** File exists on disk but not in git history (counts as not committed). Git exits non-zero (e.g., in a non-git directory or submodule not initialized) — must return `false`, not `true`.

---

### TC-2: All five spike questions answered with evidence
**Layer:** Acceptance  
**Verifies:** AC — "All five questions above answered with evidence"  
**Condition:** Read the decision document; locate each of the five questions from the spike issue  
**Expected:** Each question has a direct answer AND a supporting source (source code, issue tracker link, documentation, or ecosystem precedent). "Unknown" or "TBD" without evidence fails. Q2 ("What is the right KSP processor architecture for this use case?") must be explicitly addressed — either as a dedicated section or as an explicit statement that the question is moot because KSP is rejected. Silence on Q2 fails this TC.  
**Implementation note:** `withClue` labels in the test must align with the actual question numbering from the issue (Q1 through Q5), not the document's section ordering (which differs). Mislabeled clues obscure which question is being checked and make failures harder to diagnose.  
**Edge cases:** Answer references a GitHub issue number without a URL — acceptable if traceable; dead link — fails. Q2 answered only implicitly by the Verdict without an explicit statement — fails.

---

### TC-3: KSP not recommended — alternative named with rationale
**Layer:** Acceptance  
**Verifies:** AC — "If KSP is not recommended, the alternative is named with rationale"  
**Condition:** Decision document states whether KSP is recommended or rejected, and if rejected, names the alternative  
**Expected:** Document rejects KSP (or recommends it). If rejected: names the alternative (standalone Gradle `@CacheableTask`), explains why KSP fails for this use case (Gradle input tracking cannot see external JSON files), and confirms the alternative resolves the failure mode.  
**Implementation note:** Avoid `(text.contains("A") || text.contains("B")) shouldBe true` — Kotest reports only `expected true but was false` with no indication of which branch failed. Instead use separate `shouldContain` calls or `shouldContainAny(listOf("A", "B"))` so the failing string is named in the output.  
**Edge cases:** Document says "KSP has issues but might work with workarounds" — this is a non-decision; Verdict must be clear

---

### TC-4: Input/output contract defined
**Layer:** Acceptance  
**Verifies:** AC — "Input/output contract for the processor (or alternative) defined in the document"  
**Condition:** Decision document has an explicit input/output contract section  
**Expected:** Contract specifies: (a) input format — what file, what schema (spirv-cross `--reflect` JSON with field names), (b) output — what Kotlin source is generated per shader. Both input schema and output shape must be specified, not just named.  
**Edge cases:** Contract says "reads JSON from spirv-cross" without showing the JSON structure — incomplete; must include representative field names (ubos, push_constants, inputs, textures)

---

### TC-5: SHADER-2 issue body updated with spike findings [UPDATED — shadow from 2026-04-18 review; subprocess hardening revised 2026-04-19]
**Layer:** Acceptance  
**Verifies:** AC — "SHADER-2 issue updated with implementation hints from findings"  
**Condition:** GitHub issue #17 (SHADER-2) **body** contains implementation notes from the spike. Checked via `gh issue view 17 --json body --jq '.body'`.  
**Expected:** Issue body references the confirmed approach (Gradle task, NOT KSP). A comment does not satisfy this — the body itself must be edited via `gh issue edit 17 --body "..."`.  
**Implementation constraints:**  
1. **Stream draining order:** Drain stdout on a background thread (e.g., `val bodyFuture = CompletableFuture.supplyAsync { result.inputStream.bufferedReader().readText() }`) and call `waitFor(30, TimeUnit.SECONDS)` on the main thread. Do NOT call `readText()` before `waitFor()` — `readText()` blocks until process exit and defeats the timeout on a subprocess hang.  
2. **On timeout:** Call `result.destroyForcibly()` before failing the test. Do not leave the orphaned process running.  
3. **Environment scoping:** Preserve `PATH` and `HOME` from the parent process; remove all others; set only `GH_TOKEN`. Correct pattern: `it.clear(); it["PATH"] = System.getenv("PATH") ?: "/usr/bin:/bin"; it["HOME"] = System.getenv("HOME") ?: ""; it["GH_TOKEN"] = token`. Without `PATH`, the bare `gh` command cannot be resolved. Without `HOME`, `gh` cannot locate `~/.config/gh/` and may fail authentication even with a valid token.  
4. **Exit code:** Check `result.exitValue() == 0` before asserting on body content.  
5. **Cross-spec consistency:** This same hardening (redirectErrorStream(true), PATH+HOME+GH_TOKEN env, 30s waitFor timeout, exitValue check, destroyForcibly on timeout) must be applied to ALL `gh` subprocess invocations across spike specs — including SpikeShader1DecisionSpec's TC-5b. Inconsistent subprocess handling between specs is a test reliability defect.  
**Edge cases:** Findings posted only as a comment while body retains "TBD" — fails. `gh` subprocess exits non-zero — exit code must be checked before asserting on body content. `gh` not found on PATH — IOException before assertions; use absolute path or verify PATH is set.

---

## Design Contract

### TC-6: KSP rejected on Gradle input tracking grounds — not just complexity
**Layer:** Design  
**Verifies:** Decision — KSP fails because Gradle cannot track external JSON inputs, causing silent incremental build failures  
**Condition:** Decision document explains the specific KSP failure mode  
**Expected:** Document demonstrates that changing a `.json` file (spirv-cross output) does not trigger a KSP re-run, and that there is no supported KSP API to declare this dependency to Gradle. The failure is silent (no build error; stale types silently survive). Reference to KSP issue #1677 or equivalent structural evidence.  
**Implementation note:** Avoid multi-branch OR assertions. Prefer separate `shouldContain` calls or `shouldContainAny(...)` so Kotest names the failing string in its output.  
**Edge cases:** Document says "KSP is more complex" as the primary reason — complexity alone does not justify rejection; the correctness failure must be the lead argument

---

### TC-7: Gradle task approach correctly declares external file inputs
**Layer:** Design  
**Verifies:** Decision — `@CacheableTask` with `@InputFiles` solves the incremental build problem KSP cannot solve  
**Condition:** Task skeleton in decision document shows input declaration  
**Expected:** Skeleton uses `@InputFiles @SkipWhenEmpty @PathSensitive(PathSensitivity.RELATIVE)` for the reflection JSON files. `InputChanges` is used in `@TaskAction` to process only changed files incrementally. This directly contrasts with KSP's inability to declare external inputs.  
**Edge cases:** Skeleton uses `@InputFiles` but not `@PathSensitive` — acceptable with a note; skeleton has no incremental support — fails

---

### TC-8: Three-stage task pipeline is defined
**Layer:** Design  
**Verifies:** Decision — pipeline is compileShaders → reflectShaders → generateBindings → compileKotlin  
**Condition:** Decision document describes the full task chain  
**Expected:** Document explicitly shows all four stages with their dependencies. `reflectShaders` task runs `spirv-cross --reflect` and produces `.reflection.json` per shader. `generateShaderBindings` consumes JSON and produces `.kt` files. `compileKotlin` sees generated sources automatically via source set wiring.  
**Implementation note:** Avoid OR assertions for stage names. Use separate `shouldContain` calls per stage so each failing stage is named independently.  
**Edge cases:** Document shows three stages but omits `reflectShaders` (conflating it with compileShaders) — fails; reflection is a distinct stage

---

### TC-9: Generated types use correct Kotlin idioms
**Layer:** Design  
**Verifies:** Decision — `@JvmInline value class` for bindings, `sealed interface` for vertex attributes, `data class` for push constants  
**Condition:** Decision document shows example generated output for a shader with at least one UBO, one push constant, and one vertex input  
**Expected:** Generated `@JvmInline value class` for each binding index (not a raw Int). Vertex attributes as `sealed interface` with one subtype per location. Push constants as `data class`. Each type is in `commonMain` so all KMP targets see it.  
**Implementation note:** The check must be scoped to the **generated output code block** specifically — not any code block in the document. The task skeleton (block 4) and registration snippet (block 5) may plausibly contain `data class` or `sealed` in future edits. Add a `hasCodeBlockNamed(sectionHeader, content)` helper that locates the code block under the "Output contract" or "generated output example" section, or extract the block by its ordinal position and assert within it.  
**Edge cases:** Generated class uses a bare `Int` property for binding index instead of `@JvmInline value class` — fails; "the whole point" of this feature is no raw int exposure

---

### TC-10: Source set wiring — generated Kotlin sources visible to compileKotlin
**Layer:** Design  
**Verifies:** Decision — `outputDir` of `generateShaderBindings` is added to `commonMain.kotlin.srcDirs`  
**Condition:** Decision document or registration snippet shows source set wiring  
**Expected:** `kotlin.sourceSets.named("commonMain") { kotlin.srcDir(generateShaderBindings.map { it.outputDir }) }` or equivalent. Generated files must be in `commonMain` (not a JVM-only source set) to maintain KMP portability.  
**Implementation note:** Avoid OR assertions for wiring method names. Prefer separate `shouldContain` calls or named `withClue` per variant so failure output identifies which exact expression is missing.  
**Edge cases:** Wiring adds to `jvmMain` instead of `commonMain` — fails for KMP; generated sources in `commonMain` is a settled architectural requirement

---

### TC-11: spirv-cross chosen over spirv-reflect-kt for v0 — rationale stated
**Layer:** Design  
**Verifies:** Decision — open question resolved: spirv-cross CLI preferred over spirv-reflect-kt JVM library for initial implementation  
**Condition:** Decision document addresses the spirv-cross vs. spirv-reflect-kt choice  
**Expected:** Document explicitly states which tool is used for v0 and why. If spirv-cross: rationale is that it's already installed by the Vulkan SDK alongside glslc (no new dependency). spirv-reflect-kt noted as a valid future migration path.  
**Implementation note:** Avoid OR assertions for Vulkan SDK rationale phrases. Use separate `shouldContain` calls or `shouldContainAny(listOf(...))` with a meaningful clue that names the accepted phrases.  
**Edge cases:** Document leaves the choice open ("either works") — fails; the spike must produce a decision

---

### TC-12: Determinism invariant stated — same JSON → same Kotlin output
**Layer:** Design  
**Verifies:** Decision — binding generation is a pure function: deterministic, idempotent  
**Condition:** Decision document states or implies the determinism property  
**Expected:** Document or skeleton confirms that for identical reflection JSON input, the generated Kotlin source is byte-for-byte identical across runs. This makes the task safely cacheable (`@CacheableTask` requires this).  
**Edge cases:** Skeleton uses a timestamp or random UUID in generated output — fails determinism and breaks build cache

---

## Failure Paths

### TC-13: Input JSON schema names required fields and parsing stub defined [SUPERSEDED by TC-16]
**Layer:** Failure  
**Verifies:** Decision — input contract names required JSON fields and the skeleton defines a parsing entry point  
**Condition:** Decision document names the five required JSON fields (entryPoints, inputs, ubos, push_constants, textures) and includes a `parseReflectionJson` method stub in the skeleton  
**Expected:** All five field names present in the JSON schema section. `parseReflectionJson` visible in a code block.  
**Edge cases:** JSON is valid but has zero descriptors (shader with no bindings) — must produce a valid empty binding object, not an error

---

### TC-16: Input JSON schema contract is defined; error handling approach is stated [ADDED — shadow from 2026-04-18 review]
**Layer:** Failure  
**Shadow:** TC-13 does not test error-handling behavior as specified  
**Verifies:** That TC-13's original intent (error detection for malformed JSON) is either addressed by the document or explicitly scoped out  
**Condition:** Decision document either (a) explicitly describes what `parseReflectionJson` does when required fields are missing — error message, fail-fast behavior — OR (b) states that error handling is deferred to the implementation phase (not in the spike deliverable scope)  
**Expected:** Document does not leave error handling behavior implicit. Either the skeleton includes a note like "validate required fields; throw with shader name on missing field" OR a section note says "detailed error handling is an implementation concern beyond the spike scope."  
**Edge cases:** Document is silent on error handling — fails; the spike must not leave this open-ended given that `@CacheableTask` + incremental builds mean silent errors cause very hard-to-debug stale-binding issues

---

### TC-14: Shader source change triggers full regeneration of its binding file
**Layer:** Failure  
**Verifies:** Decision — incremental build correctly identifies that a changed shader invalidates its downstream binding  
**Condition:** A shader source file is modified; downstream pipeline (compileShaders → reflectShaders → generateBindings) must re-run for that shader only  
**Expected:** With `InputChanges`, only the binding file for the changed shader is regenerated. The binding files for unchanged shaders are left as-is. Gradle up-to-date check prevents unnecessary recompilation.  
**Edge cases:** Binding generator runs for ALL shaders when any ONE changes — fails; incremental is required. Generator skips regeneration even when reflection JSON changed — also fails.

---

### TC-15: KDoc IDE sealed hierarchy regression is documented (KSP #1351 — informational)
**Layer:** Failure  
**Verifies:** Decision — known KSP caveat acknowledged, even though approach is rejected  
**Condition:** Decision document addresses the sealed hierarchy IDE regression  
**Expected:** Document notes that KSP-generated sealed types have a known IntelliJ recognition issue (KSP #1351) and confirms this is moot under the Gradle task approach (generated files are regular source files, no regression).  
**Implementation note:** Avoid OR assertion for "moot" / "regular source" — use separate named `shouldContain` calls with distinct `withClue` text for each phrase so failure output names the missing term.  
**Edge cases:** Document does not mention KSP #1351 at all — acceptable if KSP is cleanly rejected in TC-6; only matters if KSP was partially retained

---

### TC-17: TC-1 failure (document absent) does not cascade to all subsequent tests [ADDED — shadow from 2026-04-18 review]
**Layer:** Failure  
**Shadow:** Lazy `text` property exception corrupts all subsequent tests sharing the singleton  
**Verifies:** That `SpikeDecisionDocument.text`'s lazy initialization failure is isolated — TC-1 fails with a clear message, other TCs either skip or fail independently  
**Condition:** Decision document is absent from disk at test time  
**Expected:** TC-1 fails with "Decision document not found at `<path>`". Subsequent TCs that call `decisionDoc.text` fail with the same clear message, not a different `IllegalStateException` from cached lazy state. The suite does not produce misleading cascade failures that imply unrelated assertions failed.  
**Implementation note:** Use `LazyThreadSafetyMode.NONE` on the `text` property, or restructure tests so each TC that needs the document checks `decisionDoc.exists` first. Do not rely on Kotlin's default lazy exception caching to produce useful diagnostics.

---

### TC-18: committedToGit() returns false when git exits non-zero [ADDED — shadow from 2026-04-19 review]
**Layer:** Failure  
**Shadow:** `committedToGit()` returns `true` on git error output (exit code discarded; stderr merged to stdout satisfies `isNotBlank()`)  
**Verifies:** That `committedToGit()` correctly returns `false` on git failure, not `true` based on non-blank error output  
**Condition:** `committedToGit()` is invoked with a path that causes git to exit non-zero (e.g., a path outside any git-tracked tree, or inside an uninitialized submodule)  
**Expected:** Method returns `false`. Not `true` based on the non-blank error text ("fatal: not a git repository") merged via `redirectErrorStream(true)`.  
**Implementation fix:** After `result.waitFor(30, TimeUnit.SECONDS)`, check `result.exitValue() == 0` before evaluating `output.isNotBlank()`. If exit code is non-zero, return `false` immediately. If timeout fires, call `result.destroyForcibly()` and return `false`.  
**Edge cases:** Git exits 0 with empty output (path not in history) — correctly returns `false` via `isNotBlank()`. Git exits 128 with "fatal: not a git repository" on stderr — must return `false`, not `true`.

---

## Coverage Summary

| Layer | Count | Notes |
|---|---|---|
| Acceptance | 5 | One per AC item; TC-2 updated: explicit Q2 answer required; TC-5 subprocess hardening revised: stream order, PATH+HOME, cross-spec consistency |
| Design | 7 | KSP rejection rationale, Gradle incremental, three-stage pipeline, generated type idioms (scope-aware check), source set wiring, spirv-cross choice, determinism |
| Failure | 6 | TC-13 (scoped to schema), TC-14 (incremental), TC-15 (KSP IDE regression), TC-16 (error handling stated), TC-17 (document-absent cascade), TC-18 (committedToGit exit code) |
| **Total** | **18** | (+1 added TC-18; impl notes added to TC-1, TC-2, TC-3, TC-5, TC-6, TC-8, TC-9, TC-10, TC-11, TC-15) |
