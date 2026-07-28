# Test Plan — Issue #4: F-2
**CI pipeline — GitHub Actions + Lavapipe + validation layers**

| Field | Value |
|---|---|
| Issue | [#4](https://github.com/bigshotClay/khaos/issues/4) |
| Date | 2026-04-19 |
| Author | Sentinel |
| Design input | Issue body + implementation notes (no separate design doc) |
| Test count | 15 (7 Acceptance, 5 Design Contract, 3 Failure) |

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
**Expected:** The smoke test installs a Vulkan debug messenger callback. Any message with `VkDebugUtilsMessageSeverityFlagBitsEXT >= WARNING` and a VUID in `pMessageIdName` causes the test to fail (assertion or thrown exception). Build exits non-zero. No suppression list, no allowlist, no `@Disabled` annotation.  
**Edge cases:** VUID emitted but swallowed in callback without asserting — silent failure, fails; only `ERROR`-severity VUIDs caught (not `WARNING`) — may miss sync VUIDs, acceptable only if sync-only VUIDs are always `ERROR`

---

#### TC-6: `./gradlew test` succeeds end-to-end on CI runner
**Verifies:** AC — "`./gradlew test` runs successfully on the CI runner against a minimal smoke test"  
**Condition:** The CI workflow step `./gradlew test` (or equivalent) completes with exit code 0 on `ubuntu-latest`  
**Expected:** Kotest smoke test runs, creates a `VkInstance`, confirms Lavapipe selected, tears down cleanly. `./gradlew test` exits 0. No environment-specific failure (missing JDK, missing Vulkan headers, missing ICD).  
**Edge cases:** Gradle wrapper not committed — workflow must bootstrap it or the step fails; JDK not pre-installed on runner — workflow must install it

---

#### TC-7: CI log records validation layer version and Lavapipe driver version
**Verifies:** AC — "CI run log records the validation layer version and Lavapipe driver version for traceability"  
**Condition:** Either a workflow step or the smoke test itself prints both version strings to stdout  
**Expected:** CI log contains the Khronos validation layer version (e.g., from `dpkg -l vulkan-validationlayers` output or the layer's own `VK_LAYER_KHRONOS_validation` spec version) and the Lavapipe driver version (from `VkPhysicalDeviceProperties.driverVersion` or equivalent). Both must appear in the run log, not just be queried silently.  
**Edge cases:** Versions logged to stderr but captured — acceptable; version logged only locally (not in GH Actions run log) — fails

---

### Design Contract

#### TC-8: Mesa packages installed via `apt` (not a third-party action for Vulkan SDK)
**Verifies:** Implementation note — "Mesa installable via `apt` (`mesa-vulkan-drivers`, `vulkan-validationlayers`)"  
**Condition:** Inspect the workflow install step  
**Expected:** Workflow uses `sudo apt-get install -y mesa-vulkan-drivers vulkan-validationlayers` (or a superset). It does NOT use `jakoch/install-vulkan-sdk-action` for the Lavapipe path — that action installs the official Vulkan SDK (for `glslc`, headers) not Mesa CPU drivers. The two purposes are distinct and must not be conflated.  
**Edge cases:** Both `apt` and the Vulkan SDK action are used — acceptable if roles are separated (SDK for toolchain, `apt` for runtime ICD + layers)

---

#### TC-9: `VK_ICD_FILENAMES` points to the Lavapipe ICD JSON
**Verifies:** Implementation note — "Set `VK_ICD_FILENAMES` to point to the Lavapipe ICD JSON"  
**Condition:** The workflow sets the env var; the path resolves on `ubuntu-latest`  
**Expected:** `VK_ICD_FILENAMES` is set to `/usr/share/vulkan/icd.d/lvp_icd.x86_64.json` (or the path produced by `apt` on the runner). The path must exist after the `apt install` step. Setting it prevents any non-CPU ICD from being loaded in CI.  
**Edge cases:** Env var set to a glob or directory (not a file path) — Vulkan loader behavior undefined; path incorrect for the runner architecture — ICD not loaded, VkInstance creation fails at TC-2

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
**Condition:** Smoke test teardown sequence  
**Expected:** `vkDestroyDevice` is called before `vkDestroyInstance`. The debug messenger is destroyed (or the handle goes out of scope) before `vkDestroyInstance`. No implicit object leak. Clean teardown is itself a VUID gate condition.  
**Edge cases:** JVM finalizer handles teardown in non-deterministic order — fails; teardown inside a `use {}` or `AutoCloseable` scope that enforces order — acceptable

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
**Expected:** A test that deliberately miscalls a Vulkan function (e.g., wrong destruction order) emits a VUID. The debug messenger callback catches it. The Kotest test fails with the VUID text in the failure message. `./gradlew test` exits non-zero. The CI step reports failure.  
**Edge cases:** The intentional VUID test is tagged `@Disabled` — defeats the purpose, fails; the VUID is in a separate Gradle subproject not run by `./gradlew test` — fails if AC-6 isn't met

---

#### TC-15: Validation layer not loaded — smoke test detects absence and fails
**Verifies:** Failure mode — layer missing or env var not set  
**Condition:** Simulate missing layer by clearing `VK_INSTANCE_LAYERS` after install  
**Expected:** Smoke test enumerates available instance layers via `vkEnumerateInstanceLayerProperties` and asserts that `VK_LAYER_KHRONOS_validation` is present before proceeding. If the layer is absent, the test fails with a message stating the layer was not found — it does NOT silently skip validation and continue.  
**Edge cases:** Layer is installed but not in the instance layer list (wrong architecture package) — caught by the assertion; test asserts layer name with wrong casing — assertion passes but layer is inactive, must also verify activation at runtime

---

## Coverage Summary

| Layer | Count | Notes |
|---|---|---|
| Acceptance | 7 | One per AC item |
| Design Contract | 5 | apt vs SDK action separation, ICD path, smoke test scope, in-process VUID callback, teardown order |
| Failure | 3 | Missing ICD, VUID emitted, layer absent |
| **Total** | **15** | |
