# Test Plan — Issue #5: VK-1: VulkanOutcome sealed hierarchy

## Scope

Define and verify the `VulkanOutcome` sealed hierarchy as the typed result type for all Vulkan operations in `khaos-core`. No raw `VkResult` crosses the wrapper boundary.

## Test Cases

### Acceptance

- **TC-1: Hierarchy completeness** — Enumerate all `VkResult` codes used in the v0 kernel; assert each maps to a distinct `VulkanOutcome` subtype. Mapping table is explicit in code; missing codes are a compile error or an explicit `TODO` with a tracking comment.
- **TC-2: Primary branch coverage** — `VulkanOutcome.Success` and `VulkanOutcome.Error` exist as sealed branches; every other subtype descends from one of them.
- **TC-3: SwapchainOutOfDate naming** — `SwapchainOutOfDate` is a named subtype accessible without casting; it is not subsumed under a generic `Error`.
- **TC-4: KDoc presence** — Every public type in the hierarchy has a KDoc comment; automated KDoc check (`dokka` or linter) fails on missing docs.
- **TC-5: VkResult round-trip** — For each mapped `VkResult` code, assert `fromVkResult(code)` returns the expected `VulkanOutcome` subtype. Test table-driven with one assertion per code.
- **TC-6: No raw result crossing** — Grep assertion: no public function in `khaos-core` returns `Int` or `VkResult` (raw handle types). Confirmed by code review gate.

### Design Contract

- **TC-7: Exhaustive when compile-gate** — A `when` expression over `VulkanOutcome` without an `else` branch compiles without warning. Add a new branch at compile time; verify the existing exhaustive `when` no longer compiles until updated. (Negative compile test using a test-only subproject or annotation-based lint.)
- **TC-8: SwapchainOutOfDate is not Error** — `SwapchainOutOfDate` is NOT a subtype of `VulkanOutcome.Error`; it is reachable in a `when` branch distinct from the error branch. Assert `outcome is VulkanOutcome.Error == false` when outcome is `SwapchainOutOfDate`.
- **TC-9: No unchecked exceptions for expected codes** — All `VkResult` codes covered by the sealed hierarchy throw no `Exception` at the boundary; only truly unexpected codes (unmapped) are permitted to escalate.

### Failure Paths

- **TC-10: Unmapped VkResult code** — Verify behavior when `fromVkResult()` receives an unmapped integer code: either throws `IllegalStateException` with the code value in the message, or returns a typed `UnknownError(code)` subtype. Either policy is acceptable; behavior must be explicit and tested.
- **TC-11: Success branch is not silently returned for errors** — Pass `VK_ERROR_DEVICE_LOST` (or equivalent error code) through `fromVkResult()`; assert result is NOT `VulkanOutcome.Success`.
