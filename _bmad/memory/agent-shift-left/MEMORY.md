# Memory — Recurring Patterns and Decisions

_Distilled insights. Grows over time. Keep under 200 lines._

## Khaos Project — Key Test Oracles

- **VUID gate is non-negotiable:** Zero Vulkan VUIDs = build passes. Any VUID = build failure. The validation layer + sync validation are always on in CI.
- **GPU-free testing preferred:** Render graph and command recording are designed as pure functions — test without GPU wherever possible. Lavapipe is the fallback, not the first resort.
- **Shader pipeline determinism:** Same reflection JSON → same Kotlin source → same binary. This is a testable invariant for the binding generator.
- **`@JvmInline value class` handle safety:** Handle type mismatch is a compile error, not a runtime check. Test plans should verify this at the type system level (wrong handle type = doesn't compile).
- **`VulkanOutcome` exhaustiveness:** `when` on VulkanOutcome must be exhaustive — the sealed hierarchy enforces this at compile time.

## Recurring Vulkan Test Patterns

- **Lavapipe identity check:** Any test creating a physical device must verify the selected device is Lavapipe (`llvmpipe` in `VkPhysicalDeviceProperties.deviceName`), not assume it.
- **VUID callback must be in-process:** Set a flag or throw in `PFN_vkDebugUtilsMessengerCallbackEXT`; do NOT rely on post-test log scraping. Log scraping misses VUIDs on non-test threads.
- **Teardown order gate:** Every test plan touching VkInstance + VkDevice must include a teardown order test case (`VkDevice` → debug messenger → `VkInstance`). This is a VUID gate condition, not a style preference.
- **Infrastructure ACs → trace to artifact:** For CI/infra issues, each AC maps to a verifiable artifact (log line, file path, env var, exit code). Acceptance tests describe the artifact, not just the intent.

## Test Plan Structure Convention

- Top-level section must be `## Test Cases` (required by shift-left-dev prerequisites check)
- Organize within it using `###` sub-sections: `### Acceptance`, `### Design Contract`, `### Failure Paths`
- Do NOT use `##` layer-named sections — these break the content check

## Gauntlet Shadow Patterns (from Issue #4 Loop 1)

- **VUID severity filter must cover BOTH bits:** `WARNING` (0x00000100) and `ERROR` (0x00001000). A callback guarded only by `severity and WARNING_BIT != 0` silently drops ERROR-severity VUIDs. Always require both bits in TC that verifies VUID callback.
- **`lastIndexOf` across whole test file is dangerous:** When an intentional-failure test block contains the same Vulkan call being checked (e.g., `vkDestroyInstance`), `lastIndexOf` finds the failure test, not the happy path. Scope source inspection to the target block range, or prefer behavioral assertions (TC-14 VUID detection) over text-position checks.
- **`beforeTest` in Kotest fires for ALL tests in the spec:** If `beforeTest` makes Vulkan calls (e.g., `vkEnumerateInstanceLayerProperties`), it runs before static inspection tests too. Always scope Vulkan `beforeTest` inside a `context("Vulkan runtime")` container, or split static and runtime tests into separate spec classes.
- **`vkCreateDebugUtilsMessengerEXT` return code must be checked:** If messenger creation fails silently, the zero-VUID assertion passes trivially (no callbacks ever fire). TC-5's zero-VUID assertion is only meaningful after confirming the messenger is active.
- **CI action pinning is a design contract, not optional hardening:** All `uses:` entries must reference 40-char hex SHAs. Mutable tags (`@v4`) allow supply-chain injection without the repo's knowledge.
- **GITHUB_TOKEN write permissions default is dangerous:** No `permissions:` block = default write access. Always assert `permissions: contents: read` and that `contents: write` is NOT set.
- **LWJGL `lwjglNatives` hardcoded = platform breakage:** `"natives-linux"` hardcoded in `build.gradle.kts` breaks macOS and Windows local dev. Always require platform-conditional native classifiers or OS-detection logic.
- **`vkCreateInstance` result must be checked in every TC that uses it:** Discarding the return value means a failed creation produces no VUIDs, and the test fails for the wrong reason. TC verifying VUID emission must assert `VK_SUCCESS` as a precondition.

## Gauntlet Shadow Patterns (from Issue #4 Loop 2)

- **Shadow fixes must propagate to ALL test contexts:** When a fix is applied (e.g., assert messenger VK_SUCCESS in the main test), every other test that makes the same call must apply the same requirement. TC-14 reintroduced the G-07 failure mode because it made the same `vkCreateDebugUtilsMessengerEXT` call without the assertion. When writing test plan updates from shadows, check all TCs that touch the same API.
- **G-02 try/finally applies to ALL Vulkan handles, not just callbacks:** `VkInstance`, `VkDevice`, `VkDebugUtilsMessengerCallbackEXT` — any handle created in a test body where assertions can throw between creation and cleanup must be protected by try/finally. The test plan should state this explicitly, not assume the developer will generalize from the callback case.

## Issue Sequencing (v0-kernel, as of 2026-04-18)
- Spikes #1 and #2: design decisions written; issues still OPEN (not closed yet)
- Active path: F-1 (#3) → F-2 (#4) → VK-1/VK-2 (#5/#6) → VK-3 (#7) → VK-4 (#8) → VK-5 (#9) → ...
- SHADER-1 (#16) + SHADER-2 (#17): blocked labels still on; spike decisions exist and unblock them

## Recurring Shadow Patterns (from Gauntlet reviews)

- **Issue-update AC:** "SHADER-2 issue updated" → write "issue BODY must be edited via `gh issue edit`" not "comment posted". Comment ≠ body update.
- **Subprocess deadlock:** Any `ProcessBuilder` with `redirectErrorStream(false)` reading stdout only is a deadlock risk if stderr fills the OS pipe buffer. Always specify `redirectErrorStream(true)` or explicit stderr drain in implementation notes.
- **Subprocess stream order:** `readText()` BEFORE `waitFor(timeout)` defeats the timeout — `readText()` blocks until process exit. The correct pattern: drain stdout on a background thread; call `waitFor(30, SECONDS)` on the calling thread. Always specify this order explicitly.
- **Subprocess environment:** `it.clear()` removes `PATH` (binary resolution fails) and `HOME` (gh config fails). Preserved variables: `PATH`, `HOME`, `GH_TOKEN`. All others removed. Apply to ALL `gh` subprocess calls across ALL spike specs — cross-spec inconsistency is a test reliability defect.
- **Subprocess exit code:** `output.isNotBlank()` as return value is wrong when stderr is merged — git/gh error text satisfies it. Always check `exitValue() == 0` first.
- **Subprocess timeout on all callers:** Adding `waitFor(30, SECONDS)` to one subprocess call (TC-5) does not automatically fix others (committedToGit). Specify timeout consistently across all ProcessBuilder uses.
- **Lazy singleton cascade:** File-scoped `lazy { check(exists) }` properties cache and re-throw exceptions. A missing document cascades `IllegalStateException` across all tests. Spike doc tests should note: use `LazyThreadSafetyMode.NONE` or per-test existence checks.
- **OR-chain assertions:** `(text.contains("A") || text.contains("B")) shouldBe true` gives zero branch attribution on failure. Add implementation note: use separate `shouldContain` calls or `shouldContainAny(listOf("A","B"))` with named clues. Flag this in any TC with an OR-branch assertion.
- **hasCodeBlock scope:** `hasCodeBlock()` scans ALL code blocks. When a TC says "in the generated output code block", add an implementation note requiring a scope-aware check (by section header or block ordinal).
- **Spike Failure TC scope inflation:** Spike deliverable = decision doc, not production code. *Failure* TCs should verify "doc addresses the failure mode" not "implementation validates and throws." Scope to what the spike artifact actually delivers.
