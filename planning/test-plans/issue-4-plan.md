# Test Plan — Issue #4: F-2
**CI pipeline — GitHub Actions + Lavapipe + validation layers**

| Field | Value |
|---|---|
| Issue | [#4](https://github.com/bigshotClay/khaos/issues/4) |
| Date | 2026-04-19 |
| Author | Sentinel |
| Design input | Issue body + implementation notes (no separate design doc) |
| Test count | 20 (7 Acceptance, 8 Design Contract, 5 Failure) |

---

## Test Cases

### Acceptance

#### TC-1: `ci.yml` exists at the correct path with the correct triggers
**Verifies:** AC — "`.github/workflows/ci.yml` defined; triggers on push to `main` and on pull requests"  
**Condition:** Read `.github/workflows/ci.yml`; inspect the `on:` block  
**Expected:** File exists at exactly `.github/workflows/ci.yml`. The `on:` block contains both `push: branches: [main]` and `pull_request:`. No typo in branch name; no missing trigger.  
**Edge cases:** File exists at `.github/workflows/CI.yml` (case difference) — fails on case-sensitive Linux runners; `push:` with no branch filter (triggers on all branches) — acceptable but not preferred; `workflow_dispatch:` additional trigger — fine

---

#### TC-2: Lavapipe installed and verified active on the CI runner
**Verifies:** AC — "Lavapipe (Mesa CPU Vulkan) installed and verified active on the CI runner"  
**Condition:** The workflow installs Mesa packages and sets `VK_ICD_FILENAMES`; CI log contains Lavapipe adapter confirmation  
**Expected:** Workflow installs `mesa-vulkan-drivers` and `libvulkan1` (at minimum) via `apt`. `VK_ICD_FILENAMES` is set to the Lavapipe ICD JSON (e.g., `/usr/share/vulkan/icd.d/lvp_icd.x86_64.json`). The smoke test confirms the selected physical device is `llvmpipe` (or equivalent Lavapipe identifier).  
**Edge cases:** `VK_ICD_FILENAMES` not set but Lavapipe ICD is auto-discovered — acceptable only if log confirms Lavapipe is actually selected; any discrete GPU inadvertently selected — fails

---

#### TC-3: `VK_LAYER_KHRONOS_validation` loaded and confirmed active
**Verifies:** AC — "`VK_LAYER_KHRONOS_validation` loaded and confirmed active in CI"  
**Condition:** The workflow installs the validation layer package and sets the appropriate environment variable(s); the smoke test confirms layer presence  
**Expected:** `vulkan-validationlayers` installed via `apt`. `VK_INSTANCE_LAYERS=VK_LAYER_KHRONOS_validation` set in the workflow environment. Smoke test (or layer loader log) confirms `VK_LAYER_KHRONOS_validation` appears in the active instance layer list at runtime.  
**Edge cases:** Layer is installed but env var not set — layer not active, fails; layer name typo (`VK_LAYER_KHRONOS_Validation`) — silently skipped by loader, fails

---

#### TC-4: Synchronization validation enabled
**Verifies:** AC — "Synchronization validation enabled (`VK_VALIDATION_FEATURE_ENABLE_SYNCHRONIZATION_VALIDATION_EXT`)"  
**Condition:** Smoke test enables sync validation via `VkValidationFeaturesEXT` at `VkInstance` creation  
**Expected:** `VkValidationFeaturesEXT` struct is chained into `VkInstanceCreateInfo.pNext`. `VK_VALIDATION_FEATURE_ENABLE_SYNCHRONIZATION_VALIDATION_EXT` is listed in `pEnabledValidationFeatures`. Sync validation is not settable via a mere env var at the feature granularity — struct must be present.  
**Edge cases:** `VK_LAYER_ENABLES=VALIDATION_CHECK_ENABLE_VENDOR_SPECIFIC_ARM` or similar — irrelevant; only the struct-enabled sync validation flag satisfies this AC

---

#### TC-5: Zero VUIDs = build passes; any VUID = build failure
**Verifies:** AC — "Zero VUIDs = build passes; any VUID = build failure (not a warning, never suppressed)"  
**Condition:** VUID detection causes a non-zero exit code from `./gradlew test`  
**Expected:** The smoke test installs a Vulkan debug messenger callback. The callback severity condition MUST include BOTH `VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT` (0x00000100) AND `VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT` (0x00001000) — checking only the WARNING bit silently drops ERROR-severity VUIDs. Any message matching either bit with a VUID in `pMessageIdName` causes the test to fail. Build exits non-zero. No suppression list, no allowlist, no `@Disabled` annotation. Note: the messenger must be confirmed active (TC-18) before this zero-VUID assertion is meaningful.  
**Edge cases:** VUID emitted but swallowed in callback without asserting — silent failure, fails; callback condition uses only WARNING bit — ERROR-severity VUIDs (e.g., 0x00001000) pass through undetected, violates zero-VUID policy

---

#### TC-6: `./gradlew test` succeeds end-to-end on CI runner
**Verifies:** AC — "`./gradlew test` runs successfully on the CI runner against a minimal smoke test"  
**Condition:** The CI workflow step `./gradlew test` (or equivalent) completes with exit code 0 on `ubuntu-latest`  
**Expected:** Kotest smoke test runs, creates a `VkInstance`, confirms Lavapipe selected, tears down cleanly. `./gradlew test` exits 0. No environment-specific failure (missing JDK, missing Vulkan headers, missing ICD).  
**Edge cases:** Gradle wrapper not committed — workflow must bootstrap it or the step fails; JDK not pre-installed on runner — workflow must install it

---

#### TC-7: CI log records validation layer version and Lavapipe driver version
**Verifies:** AC — "CI run log records the validation layer version and Lavapipe driver version for traceability"  
**Condition:** The dedicated TC-7 test body (not only the main smoke test) prints both version strings to stdout  
**Expected:** The TC-7 test logs the Khronos validation layer version AND the Lavapipe driver version in human-readable form. The Lavapipe driver version must NOT be logged as a raw `uint32`. Required: decode `driverVersion()` with VK_VERSION_MAJOR/MINOR/PATCH bit operations or use `VkPhysicalDeviceDriverProperties.driverInfo`. Both version strings must be non-empty and appear in the GH Actions run log. RESOURCE SAFETY (L2-02): if the TC-7 test body creates a `VkInstance` to enumerate physical devices for the driver version query, that instance MUST be protected by a `try/finally` guard ensuring `vkDestroyInstance` is called regardless of assertion outcomes. The same G-02 resource-safety pattern (try/finally for all Vulkan handle cleanup) applies to any VkInstance created in TC-7, not only to native callback allocations.  
**Edge cases:** Versions logged only in the main test body — fails AC traceability; raw uint32 logged without decoding — fails; VkInstance created in TC-7 without try/finally — instance leaked on assertion failure between creation and destroy

---

### Design Contract

#### TC-8: Mesa packages installed via `apt` (not a third-party action for Vulkan SDK)
**Verifies:** Implementation note — "Mesa installable via `apt` (`mesa-vulkan-drivers`, `vulkan-validationlayers`)"  
**Condition:** Inspect the workflow install step  
**Expected:** Workflow uses `sudo apt-get install -y mesa-vulkan-drivers vulkan-validationlayers` (or a superset). It does NOT use `jakoch/install-vulkan-sdk-action` for the Lavapipe path — that action installs the official Vulkan SDK (for `glslc`, headers) not Mesa CPU drivers. The two purposes are distinct and must not be conflated.  
**Edge cases:** Both `apt` and the Vulkan SDK action are used — acceptable if roles are separated (SDK for toolchain, `apt` for runtime ICD + layers)

---

#### TC-9: `VK_ICD_FILENAMES` points to the Lavapipe ICD JSON — path discovered dynamically
**Verifies:** Implementation note — "Set `VK_ICD_FILENAMES` to point to the Lavapipe ICD JSON"  
**Condition:** The workflow sets the env var; the path resolves on `ubuntu-latest` without hardcoding the architecture suffix  
**Expected:** `VK_ICD_FILENAMES` is set to the Lavapipe ICD path. The path MUST be discovered dynamically — the workflow must NOT hardcode the architecture suffix `x86_64` in the ICD path. Required: the `VK_ICD_FILENAMES` assignment uses a shell glob or discovery command (e.g., `$(ls /usr/share/vulkan/icd.d/lvp_icd.*.json | head -1)`). Static inspection: assert that the literal string `x86_64` does NOT appear in the `VK_ICD_FILENAMES` assignment line in `ci.yml`. This ensures ARM64 runners (which install `lvp_icd.aarch64.json`) are not broken by the hardcoded suffix.  
**Edge cases:** Hardcoded `x86_64` path on x86_64 runner — works locally but breaks ARM64 CI; glob matching multiple files — `head -1` selects one deterministically; path set to a directory — Vulkan loader behavior undefined

---

#### TC-10: Smoke test scope — VkInstance creation only, not a full pipeline
**Verifies:** Implementation note — "A 'can we create a `VkInstance`?' smoke test is sufficient here — full rendering tests come in TEST-4"  
**Condition:** Inspect the smoke test; verify it does not attempt swapchain, render pass, or framebuffer creation  
**Expected:** Smoke test creates a `VkInstance` (with validation + sync validation features), enumerates physical devices, selects Lavapipe, creates a `VkDevice`, then destroys both in order. No surface, no swapchain, no command buffer, no render pass. The scope boundary is explicit — more would be premature.  
**Edge cases:** Smoke test also creates a `VkCommandPool` for completeness — acceptable only if it contributes to VUID coverage; creating a surface via headless extension — acceptable if done intentionally for F-3 prep, must be documented

---

#### TC-11: VUID callback asserts in-process, not post-hoc log scraping
**Verifies:** Design decision — build failure on VUID must be deterministic and in-process  
**Condition:** Inspect the debug messenger callback and the test assertion strategy  
**Expected:** The Vulkan debug messenger callback is set as a `PFN_vkDebugUtilsMessengerCallbackEXT`. When called with a VUID, the callback sets an atomic flag or throws an exception that causes the Kotest assertion to fail at the end of the test. It does NOT rely on post-test log scraping (e.g., grepping stdout for "VUID"). Log scraping can miss VUIDs emitted on a non-test thread.  
**Edge cases:** Callback sets a flag but test never checks the flag — silent VUID, fails; callback panics instead of setting flag — acceptable if exception propagates to test failure

---

#### TC-12: Teardown order — `VkDevice` destroyed before `VkInstance`
**Verifies:** VUID `VUID-vkDestroyInstance-instance-00629` — all child objects must be destroyed before `VkInstance`  
**Condition:** Smoke test teardown sequence, verified via behavioral VUID detection (TC-14) rather than brittle source text search  
**Expected:** `vkDestroyDevice` is called before `vkDestroyInstance` in the happy-path test. The debug messenger is destroyed before `vkDestroyInstance`. Clean teardown is itself a VUID gate condition. If source-text inspection is used to verify order, the search MUST be scoped to the happy-path test block only — `lastIndexOf("vkDestroyInstance")` across the whole file finds TC-14's intentionally wrong-order call (not the happy-path teardown) and produces a vacuous passing assertion. Preferred implementation: rely on TC-14's behavioral VUID detection as the definitive teardown-order check and remove any whole-file text-position search.  
**Edge cases:** JVM finalizer handles teardown in non-deterministic order — fails; whole-file `lastIndexOf` passes trivially because TC-14's line is always after TC-2's `vkDestroyDevice` — vacuous, must not be used; `use {}` / `AutoCloseable` scope enforcing order — acceptable

---

### Failure Paths

#### TC-13: Lavapipe not installed — smoke test fails with a diagnostic error, not a silent GPU fallback
**Verifies:** Failure mode — ICD not present  
**Condition:** Simulate missing ICD by unsetting `VK_ICD_FILENAMES` and removing installed packages (or test with wrong path)  
**Expected:** `vkCreateInstance` returns `VK_ERROR_INCOMPATIBLE_DRIVER` or physical device enumeration returns zero devices. The smoke test detects this condition explicitly and fails with a message identifying the missing ICD — not a NullPointerException from a null physical device handle.  
**Edge cases:** System has a real GPU and `VK_ICD_FILENAMES` is unset — real GPU might be selected instead of Lavapipe; the test must verify the selected device is Lavapipe, not assume it

---

#### TC-14: Any VUID emitted causes non-zero build exit
**Verifies:** Failure mode — VUID emitted during smoke test  
**Condition:** Intentionally trigger a known VUID in a dedicated failure-path test  
**Expected:** Two preconditions, both must be asserted before the deliberate wrong-order destruction: (1) `vkCreateInstance` return code MUST be asserted `VK_SUCCESS` (G-08 — failed instance creation produces no VUIDs); (2) `vkCreateDebugUtilsMessengerEXT` return code MUST be captured in a variable and asserted `VK_SUCCESS` (L2-01 — silently failed messenger produces no callbacks; the messenger handle must be stored, not discarded into a temporary buffer). After both preconditions pass: the deliberately wrong destruction order emits a VUID. The callback captures it. Assert `vuidViolation.get()` is non-null (G-01). Kotest test fails with the VUID text in the failure message. `./gradlew test` exits non-zero.  
**Edge cases:** `vkCreateInstance` return value discarded — wrong failure reason on layer/driver error; messenger result discarded — same as G-07 failure mode, VUID detection silently broken; callback only covers WARNING bit — ERROR-severity VUIDs not captured; intentional VUID test tagged `@Disabled` — defeats the purpose

---

#### TC-15: Validation layer not loaded — smoke test detects absence and fails; `beforeTest` scoped to Vulkan tests only
**Verifies:** Failure mode — layer missing or env var not set; also verifies that static tests are isolated from Vulkan runtime state  
**Condition:** Simulate missing layer by running without `VK_LAYER_KHRONOS_validation` active  
**Expected:** Smoke test enumerates available instance layers via `vkEnumerateInstanceLayerProperties` and asserts `VK_LAYER_KHRONOS_validation` is present before proceeding. If the layer is absent, the test fails explicitly with the layer name in the failure message — not silently. CRITICAL SCOPE CONSTRAINT (G-04): the `beforeTest` block that calls `vkEnumerateInstanceLayerProperties` MUST NOT execute for static inspection tests (TC-1, TC-8–TC-12, TC-17, TC-19, TC-20). A Vulkan layer misconfiguration must not cause static tests to fail with "layer not found." Required implementation: either (a) wrap all Vulkan runtime tests in a `context("Vulkan runtime") { beforeTest { ... } ... }` container, or (b) move static tests to a separate spec class that has no `beforeTest` Vulkan calls.  
**Edge cases:** `beforeTest` fires for all tests — static tests fail with Vulkan errors instead of their actual assertions (G-04 regression); layer installed but not in enumerated list (wrong architecture package) — caught by assertion; wrong casing in layer name — layer silently inactive

---

#### TC-16: Cleanup on assertion failure — native callback freed, no JVM crash
**Verifies:** Failure mode — native off-heap memory not leaked when an assertion fails mid-test (G-02)  
**Condition:** Simulate a failed `vkCreateInstance` by requesting an invalid layer name; observe test teardown  
**Expected:** When `vkCreateInstance` returns a non-SUCCESS code, the `VkDebugUtilsMessengerCallbackEXT` off-heap object is still freed and no `UnsatisfiedLinkError` or native memory corruption appears in the test run output. The test fails cleanly (non-zero exit) without a JVM crash. The `MemoryStack.use {}` block or native callback allocation MUST have a try/finally guard so the callback is freed regardless of how control exits the block.  
**Edge cases:** `MemoryStack.use {}` without try/finally — callback leaked on assertion throw; test fails cleanly but leaves callback dangling — subsequent tests may see corrupted off-heap state

---

#### TC-17: LWJGL natives are platform-conditional, not hardcoded to `natives-linux`
**Verifies:** Design Contract — `build.gradle.kts` must support local development on macOS and Windows (G-03)  
**Condition:** Static inspection of `build.gradle.kts`  
**Expected:** `lwjglNatives` is NOT set to the literal `"natives-linux"`. Required: either (a) `lwjglNatives` is computed dynamically based on `System.getProperty("os.name")` or equivalent OS detection, or (b) all three platform classifiers are declared (at minimum: `natives-linux`, `natives-macos-arm64` or `natives-macos`, `natives-windows`). Hardcoding `"natives-linux"` causes `UnsatisfiedLinkError` on macOS and Windows local development environments.  
**Edge cases:** LWJGL BOM with platform detection built-in — acceptable if it resolves the correct classifier per OS; single `natives-linux` classifier with a comment explaining CI-only intent — fails (intent ≠ contract)

---

#### TC-18: `vkCreateDebugUtilsMessengerEXT` return code asserted `VK_SUCCESS`
**Verifies:** Design Contract — the zero-VUID policy (AC-5) is only meaningful when the messenger is confirmed active (G-07)  
**Condition:** Inspect smoke test implementation; verify return code is captured and asserted  
**Expected:** The return value of `vkCreateDebugUtilsMessengerEXT` is stored and asserted equal to `VK_SUCCESS` before any VUID-dependent assertions are made. If messenger creation silently fails (handle = 0), no VUID callbacks fire and the zero-VUID assertion passes trivially — a false green. The assertion must precede the code-under-test, not follow it.  
**Edge cases:** Return value discarded — messenger silently inactive, TC-5 passes vacuously; messenger creation checked only in a log message but not asserted — fails

---

#### TC-19: GitHub Actions `uses:` entries pinned to SHA digests, not mutable version tags
**Verifies:** Design Contract — CI supply-chain security (G-09)  
**Condition:** Static inspection of `.github/workflows/ci.yml`  
**Expected:** Every `uses:` line references a full 40-character hex SHA digest (e.g., `actions/checkout@abc123...def456`), not a mutable version tag like `@v4`. Mutable tags allow a compromised action release to inject code into the CI runner without the repository's knowledge. Pattern to assert: each `uses:` value matches `[a-zA-Z0-9._/-]+@[0-9a-f]{40}`.  
**Edge cases:** SHA is correct length but not hex — fails pattern; action uses a branch ref (e.g., `@main`) — equally mutable, fails; `@v4` with a SHA comment above — the comment is not enforced, fails

---

#### TC-20: `ci.yml` declares a `permissions:` block with least-privilege `contents: read`
**Verifies:** Design Contract — GITHUB_TOKEN permissions must follow least-privilege (G-11)  
**Condition:** Static inspection of `.github/workflows/ci.yml`  
**Expected:** `ci.yml` contains a `permissions:` block. The block sets `contents: read` (minimum required for checkout). `contents: write` MUST NOT be present. Without a `permissions:` block, GitHub Actions grants default repository permissions which may include `contents: write`, amplifying the blast radius of any injection finding (G-09).  
**Edge cases:** `permissions:` block present but empty — defaults apply, may grant write; `contents: write` set for a legitimate reason (e.g., release publishing) — fails for a CI-only smoke pipeline; job-level `permissions:` overrides workflow-level — assert at whichever level applies

---

## Coverage Summary

| Layer | Count | Notes |
|---|---|---|
| Acceptance | 7 | One per AC item |
| Design Contract | 8 | apt vs SDK action separation, ICD path (dynamic), smoke test scope, in-process VUID callback, teardown order, LWJGL platform natives, messenger return code, CI SHA pinning, GITHUB_TOKEN permissions |
| Failure | 5 | Missing ICD, VUID emitted, layer absent, native cleanup on assertion failure, messenger creation failure |
| **Total** | **20** | Loop 1 shadows: G-01→TC-5/TC-14, G-02→TC-16, G-03→TC-17, G-04→TC-15, G-05→TC-12, G-06→TC-7, G-07→TC-18, G-08→TC-14, G-09→TC-19, G-10→TC-9, G-11→TC-20. Loop 2 shadows: L2-01→TC-14 (messenger result precondition), L2-02→TC-7 (VkInstance try/finally) |
