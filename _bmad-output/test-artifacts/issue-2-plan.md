# Test Plan — Issue #2: SPIKE-SHADER-2
**Validate KSP processor for SPIR-V reflection → typed Kotlin bindings**

| Field | Value |
|---|---|
| Issue | [#2](https://github.com/bigshotClay/khaos/issues/2) |
| Date | 2026-04-18 |
| Author | Sentinel |
| Design input | `_bmad-output/planning-artifacts/designs/spike-shader-2-decision.md` |
| Test count | 15 (5 Acceptance, 7 Design, 3 Failure) |

---

## Acceptance Coverage

### TC-1: Decision document committed at the agreed path
**Layer:** Acceptance  
**Verifies:** AC — "Decision document committed to the agreed path"  
**Condition:** Check that `_bmad-output/planning-artifacts/designs/spike-shader-2-decision.md` exists and is non-empty  
**Expected:** File exists, contains a Verdict section, and is committed to version control  
**Edge cases:** File exists on disk but not in git history (counts as not committed)

---

### TC-2: All five spike questions answered with evidence
**Layer:** Acceptance  
**Verifies:** AC — "All five questions above answered with evidence"  
**Condition:** Read the decision document; locate each of the five questions from the spike issue  
**Expected:** Each question has a direct answer AND a supporting source (source code, issue tracker link, documentation, or ecosystem precedent). "Unknown" or "TBD" without evidence fails.  
**Edge cases:** Answer references a GitHub issue number without a URL — acceptable if traceable; dead link — fails

---

### TC-3: KSP not recommended — alternative named with rationale
**Layer:** Acceptance  
**Verifies:** AC — "If KSP is not recommended, the alternative is named with rationale"  
**Condition:** Decision document states whether KSP is recommended or rejected, and if rejected, names the alternative  
**Expected:** Document rejects KSP (or recommends it). If rejected: names the alternative (standalone Gradle `@CacheableTask`), explains why KSP fails for this use case (Gradle input tracking cannot see external JSON files), and confirms the alternative resolves the failure mode.  
**Edge cases:** Document says "KSP has issues but might work with workarounds" — this is a non-decision; Verdict must be clear

---

### TC-4: Input/output contract defined
**Layer:** Acceptance  
**Verifies:** AC — "Input/output contract for the processor (or alternative) defined in the document"  
**Condition:** Decision document has an explicit input/output contract section  
**Expected:** Contract specifies: (a) input format — what file, what schema (spirv-cross `--reflect` JSON with field names), (b) output — what Kotlin source is generated per shader. Both input schema and output shape must be specified, not just named.  
**Edge cases:** Contract says "reads JSON from spirv-cross" without showing the JSON structure — incomplete; must include representative field names (ubos, push_constants, inputs, textures)

---

### TC-5: SHADER-2 updated with spike findings
**Layer:** Acceptance  
**Verifies:** AC — "SHADER-2 issue updated with implementation hints from findings"  
**Condition:** GitHub issue #17 (SHADER-2) has implementation notes from the spike — either updated body or linked comment  
**Expected:** SHADER-2 implementation notes reference the confirmed approach (Gradle task, NOT KSP) and remove or supersede the KSP-specific AC items with the alternative approach  
**Edge cases:** SHADER-2 still says "KSP" in its implementation notes after the spike — fails; the spike must have propagated its decision

---

## Design Contract

### TC-6: KSP rejected on Gradle input tracking grounds — not just complexity
**Layer:** Design  
**Verifies:** Decision — KSP fails because Gradle cannot track external JSON inputs, causing silent incremental build failures  
**Condition:** Decision document explains the specific KSP failure mode  
**Expected:** Document demonstrates that changing a `.json` file (spirv-cross output) does not trigger a KSP re-run, and that there is no supported KSP API to declare this dependency to Gradle. The failure is silent (no build error; stale types silently survive). Reference to KSP issue #1677 or equivalent structural evidence.  
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
**Edge cases:** Document shows three stages but omits `reflectShaders` (conflating it with compileShaders) — fails; reflection is a distinct stage

---

### TC-9: Generated types use correct Kotlin idioms
**Layer:** Design  
**Verifies:** Decision — `@JvmInline value class` for bindings, `sealed interface` for vertex attributes, `data class` for push constants  
**Condition:** Decision document shows example generated output for a shader with at least one UBO, one push constant, and one vertex input  
**Expected:** Generated `@JvmInline value class` for each binding index (not a raw Int). Vertex attributes as `sealed interface` with one subtype per location. Push constants as `data class`. Each type is in `commonMain` so all KMP targets see it.  
**Edge cases:** Generated class uses a bare `Int` property for binding index instead of `@JvmInline value class` — fails; "the whole point" of this feature is no raw int exposure

---

### TC-10: Source set wiring — generated Kotlin sources visible to compileKotlin
**Layer:** Design  
**Verifies:** Decision — `outputDir` of `generateShaderBindings` is added to `commonMain.kotlin.srcDirs`  
**Condition:** Decision document or registration snippet shows source set wiring  
**Expected:** `kotlin.sourceSets.named("commonMain") { kotlin.srcDir(generateShaderBindings.map { it.outputDir }) }` or equivalent. Generated files must be in `commonMain` (not a JVM-only source set) to maintain KMP portability.  
**Edge cases:** Wiring adds to `jvmMain` instead of `commonMain` — fails for KMP; generated sources in `commonMain` is a settled architectural requirement

---

### TC-11: spirv-cross chosen over spirv-reflect-kt for v0 — rationale stated
**Layer:** Design  
**Verifies:** Decision — open question resolved: spirv-cross CLI preferred over spirv-reflect-kt JVM library for initial implementation  
**Condition:** Decision document addresses the spirv-cross vs. spirv-reflect-kt choice  
**Expected:** Document explicitly states which tool is used for v0 and why. If spirv-cross: rationale is that it's already installed by the Vulkan SDK alongside glslc (no new dependency). spirv-reflect-kt noted as a valid future migration path.  
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

### TC-13: Malformed or missing reflection JSON is detected early
**Layer:** Failure  
**Verifies:** Decision — binding generator fails with a clear error when input JSON is missing required fields  
**Condition:** Decision document or skeleton addresses what happens when spirv-cross produces incomplete/malformed JSON  
**Expected:** Binding generator validates required JSON fields (entryPoints, inputs, ubos, push_constants) and fails with a descriptive error naming the missing field and the source shader. It does NOT silently generate an empty or partial binding class.  
**Edge cases:** JSON is valid but has zero descriptors (shader with no bindings) — must produce a valid empty binding object, not an error

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
**Edge cases:** Document does not mention KSP #1351 at all — acceptable if KSP is cleanly rejected in TC-6; only matters if KSP was partially retained

---

## Coverage Summary

| Layer | Count | Notes |
|---|---|---|
| Acceptance | 5 | One per AC item |
| Design | 7 | KSP rejection rationale, Gradle incremental, three-stage pipeline, generated type idioms, source set wiring, spirv-cross choice, determinism |
| Failure | 3 | Malformed JSON, incremental invalidation, KSP IDE regression (informational) |
| **Total** | **15** | |
