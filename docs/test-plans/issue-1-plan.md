# Test Plan — Issue #1: SPIKE-SHADER-1
**Validate shaderc + spirv-val Gradle task integration**

| Field | Value |
|---|---|
| Issue | [#1](https://github.com/bigshotClay/khaos/issues/1) |
| Date | 2026-04-18 |
| Author | Sentinel |
| Design input | `_bmad-output/planning-artifacts/designs/spike-shader-1-decision.md` |
| Test count | 14 (5 Acceptance, 6 Design, 3 Failure) |

---

## Test Cases

### Acceptance

#### TC-1: Decision document committed at the agreed path
**Verifies:** AC — "Decision document committed to the agreed path"  
**Condition:** Check that `_bmad-output/planning-artifacts/designs/spike-shader-1-decision.md` exists and is non-empty  
**Expected:** File exists, contains a Verdict section, and is committed to version control  
**Edge cases:** File exists on disk but not in git history (counts as not committed)

---

#### TC-2: All five spike questions answered with evidence
**Verifies:** AC — "All five questions above answered with evidence"  
**Condition:** Read the decision document; locate each of the five questions from the spike issue  
**Expected:** Each question has a dedicated section with a direct answer AND a supporting source (working prototype, authoritative doc, or ecosystem precedent). "Unknown" or "TBD" without evidence fails.  
**Edge cases:** Answer references a dead link; answer is present but says only "yes/no" with no evidence — both fail

---

#### TC-3: Gradle task skeleton included
**Verifies:** AC — "Gradle task skeleton included in the document"  
**Condition:** Inspect decision document for a code block containing a Gradle task definition  
**Expected:** Document contains at minimum a `@CacheableTask` class skeleton with `@InputFiles`, `@OutputDirectory`, and a `@TaskAction` method. Both the compile task and the validation task must be represented.  
**Edge cases:** Only pseudocode or prose description present — fails; skeleton must be runnable Kotlin/Groovy

---

#### TC-4: Blocking issues or platform incompatibilities explicitly named
**Verifies:** AC — "Any blocking issues or platform incompatibilities explicitly named"  
**Condition:** Decision document has a section explicitly addressing blocking issues  
**Expected:** Section states either (a) "None — no blockers found" with rationale, or (b) enumerates specific blockers. Absence of the section fails.  
**Edge cases:** Blockers mentioned in prose but not in a dedicated section — acceptable if clearly findable

---

#### TC-5: Recommendation clearly stated and SHADER-1 updated
**Verifies:** AC — "Recommendation clearly stated" + "SHADER-1 issue updated with implementation hints"  
**Condition:** (a) Decision document has a Verdict/Recommendation with explicit proceed/reject decision. (b) GitHub issue #16 (SHADER-1) has implementation notes from spike findings — either in the issue body or as a comment.  
**Expected:** (a) Document states whether to proceed with the investigated approach or names the confirmed alternative. (b) SHADER-1 implementation notes reference the spike conclusion (glslc subprocess).  
**Edge cases:** Verdict says "proceed" but recommendation section recommends an alternative — these must be consistent

---

### Design Contract

#### TC-6: LWJGL shaderc bindings ruled out for Gradle task context
**Verifies:** Decision — LWJGL shaderc is off-table for build-time; glslc subprocess is the confirmed path  
**Condition:** Decision document addresses LWJGL's `SharedLibraryLoader` behavior in a long-lived Gradle daemon JVM  
**Expected:** Document explains why LWJGL native extraction is problematic in Gradle daemon (concurrent daemon conflicts, no ecosystem precedent) and rules out LWJGL shaderc bindings with specific rationale  
**Edge cases:** Document rules out LWJGL but doesn't explain why — acceptable only if source citation provides the explanation

---

#### TC-7: glslc subprocess approach is confirmed feasible on all three platforms
**Verifies:** Decision — glslc subprocess works headlessly on Linux, macOS, and Windows  
**Condition:** Decision document addresses platform matrix explicitly  
**Expected:** Document covers Linux CI (jakoch/install-vulkan-sdk-action), macOS (Vulkan SDK install path), and Windows (LunarG installer). Each platform has a confirmed install path and `VULKAN_SDK` env var convention.  
**Edge cases:** Only Linux CI addressed — fails; all three required

---

#### TC-8: spirv-val subprocess integration confirmed — no JVM bindings exist
**Verifies:** Decision — spirv-val runs as subprocess; no JVM wrapping alternative exists  
**Condition:** Decision document explains how spirv-val failure propagates to Gradle  
**Expected:** Document confirms LWJGL has no SPIRV-Tools bindings (LWJGL #147), spirv-val runs as subprocess via `project.exec`, and a non-zero exit code causes the Gradle build to fail automatically  
**Edge cases:** Document says spirv-val "can" be called without explaining the failure propagation mechanism — incomplete

---

#### TC-9: Incremental build support addressed
**Verifies:** Decision — compile task supports incremental builds (only recompiles changed shaders)  
**Condition:** Gradle task skeleton uses `InputChanges` or equivalent mechanism  
**Expected:** Skeleton shows `@TaskAction fun compile(changes: InputChanges)` pattern with a branch for incremental vs. full rebuild. `@CacheableTask`, `@InputFiles`, and `@OutputDirectory` annotations are all present.  
**Edge cases:** Skeleton uses `@Incremental` instead of `InputChanges` — acceptable (different API, same intent); skeleton has no incremental support — fails

---

#### TC-10: Task integrates into standard Gradle lifecycle
**Verifies:** Decision — validateShaders is wired before processResources / compileKotlin  
**Condition:** Decision document or skeleton shows how tasks plug into the build graph  
**Expected:** Document shows `tasks.named("processResources") { dependsOn(validateShaders) }` or equivalent. The compile → validate chain is explicit.  
**Edge cases:** Tasks are defined but not wired to the lifecycle — fails; documentation must show wiring, not just task definitions

---

#### TC-11: VULKAN_SDK env var convention defined for CI portability
**Verifies:** Decision — glslcPath / spirvValPath use VULKAN_SDK with a configurable override  
**Condition:** Task skeleton or registration example shows how the binary path is resolved  
**Expected:** `glslcPath` defaults to `$VULKAN_SDK/bin/glslc` with an explicit property override. Build fails early with a clear message if neither `VULKAN_SDK` nor the explicit property is set.  
**Edge cases:** Path hardcoded without fallback — fails (not portable); PATH-only resolution without VULKAN_SDK convention — acceptable but noted as weaker

---

### Failure Paths

#### TC-12: GLSL compilation error produces actionable output
**Verifies:** Decision — shader compilation errors have clear source location in failure output  
**Condition:** Decision document addresses what happens when glslc encounters invalid GLSL  
**Expected:** glslc exits non-zero; error includes source file name and line number. Gradle task propagates the failure and surfaces the error message without swallowing it.  
**Edge cases:** Error captured in a stream but not printed to Gradle output — fails; must be visible in build log

---

#### TC-13: SPIR-V validation failure fails the build
**Verifies:** Decision — spirv-val non-zero exit = build failure, not warning  
**Condition:** Decision document confirms `isIgnoreExitValue = false` or equivalent for the spirv-val exec step  
**Expected:** Any SPIR-V structural error halts the build. Document explicitly states this is a hard gate, not a warning.  
**Edge cases:** Decision says "build fails" but skeleton has `isIgnoreExitValue = true` — contradiction, fails

---

#### TC-14: VULKAN_SDK not set produces a clear early failure
**Verifies:** Decision — missing SDK env var produces a clear message, not a cryptic binary-not-found error  
**Condition:** Decision document or skeleton shows how missing VULKAN_SDK is handled  
**Expected:** Task startup validates the path (e.g. `System.getenv("VULKAN_SDK") ?: error("VULKAN_SDK not set")`) and fails immediately with a human-readable message rather than a late NullPointerException or "command not found" from the subprocess  
**Edge cases:** SDK installed but glslc missing from expected path — should also produce a clear error, not a cryptic IOException

---

## Coverage Summary

| Layer | Count | Notes |
|---|---|---|
| Acceptance | 5 | One per AC item |
| Design | 6 | Toolchain choice, incremental builds, platform matrix, lifecycle wiring, env var convention, failure propagation |
| Failure | 3 | Compilation error output, SPIR-V gate, missing SDK |
| **Total** | **14** | |
