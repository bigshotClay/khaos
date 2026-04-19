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

## Issue Sequencing (v0-kernel, as of 2026-04-18)
- Spikes #1 and #2: design decisions written; issues still OPEN (not closed yet)
- Active path: F-1 (#3) → F-2 (#4) → VK-1/VK-2 (#5/#6) → VK-3 (#7) → VK-4 (#8) → VK-5 (#9) → ...
- SHADER-1 (#16) + SHADER-2 (#17): blocked labels still on; spike decisions exist and unblock them
