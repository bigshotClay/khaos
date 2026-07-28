# Test Plan — Issue #7: VK-3: Instance and physical device bootstrap

## Scope

Typed wrappers for `VkInstance` creation and physical device selection in `khaos-core`. Requires Lavapipe on CI. VUID gate active throughout.

## Preconditions

- Lavapipe (`llvmpipe`) Vulkan driver present in CI (`jakoch/install-vulkan-sdk-action`).
- Validation layer (`VK_LAYER_KHRONOS_validation`) available.
- VUID debug messenger wired in-process for all TC-tagged as [VUID gate].

## Test Cases

### Acceptance

- **TC-1: KhaosInstance construction** — Construct a `KhaosInstance` with app name, version, required extensions (surface, platform surface), and validation layer enabled. Assert result is `VulkanOutcome.Success`. [VUID gate]
- **TC-2: Physical device enumeration** — From a valid `KhaosInstance`, enumerate physical devices. Assert at least one device is returned. Confirm the selected device is Lavapipe (`llvmpipe` in `VkPhysicalDeviceProperties.deviceName`) in CI.
- **TC-3: Typed predicate selection** — Create a device selector with a typed predicate (e.g., requires `VK_QUEUE_GRAPHICS_BIT`). Assert the predicate selects the expected Lavapipe device. Assert that a predicate matching no device returns `null` or `VulkanOutcome.Error` explicitly.
- **TC-4: Required extensions as typed constants** — Verify that surface and swapchain extension names are typed constants (not string literals in call sites). Assert the constants match the Vulkan spec strings (`VK_KHR_SURFACE_EXTENSION_NAME`, etc.).
- **TC-5: Validation layer typed flag** — `VK_LAYER_KHRONOS_validation` is enabled via a typed `ValidationLayers.Khronos` constant or equivalent — not a string literal at call sites. Confirmed by code inspection.
- **TC-6: Two instances in same process** — Construct two `KhaosInstance` objects sequentially in the same test. Assert both return `Success`. Assert no shared mutable static state (no hidden globals that would cause the second construction to fail or corrupt the first). [VUID gate]
- **TC-7: Instance destruction** — Destroy a `KhaosInstance`. Assert no VUID fires on destruction. Assert that a subsequent call on the destroyed instance returns `VulkanOutcome.Error` (not a native crash). [VUID gate]

### Design Contract

- **TC-8: VUID callback in-process** — The VUID debug messenger callback sets a boolean flag (or throws) in the test's scope — not relying on log scraping. Assert the flag is clear after a clean construction + destruction cycle.
- **TC-9: Teardown order** — Destroy `VkDevice` (if created) before debug messenger, destroy messenger before `VkInstance`. Assert zero VUIDs on teardown. [VUID gate — teardown order is a gate condition]
- **TC-10: KhaosPhysicalDevice typed properties** — `KhaosPhysicalDevice.properties` returns a typed object with `deviceName: String`, `deviceType: PhysicalDeviceType`, `apiVersion: VulkanVersion`. No raw `VkPhysicalDeviceProperties` struct exposed.

### Failure Paths

- **TC-11: Missing required extension** — Attempt `KhaosInstance` construction with a required extension that does not exist. Assert result is `VulkanOutcome.Error` with a meaningful error type (not a native crash or silent failure).
- **TC-12: Validation layer absent** — Attempt construction with validation layer requested when it is not installed. Assert graceful failure (`VulkanOutcome.Error`) rather than crash or silently skipping the layer.
- **TC-13: Empty device predicate match** — Pass a predicate that matches nothing. Assert return is `null` or a typed `NoMatchingDevice` outcome — not an index-out-of-bounds or null pointer.
