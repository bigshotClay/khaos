# Test Plan — Issue #8: VK-4: Logical device and typed queue abstraction

## Scope

Typed `KhaosDevice` and `KhaosQueue` wrappers in `khaos-core`. Queue capabilities are sealed types; type mismatch is a compile error. Requires Lavapipe on CI.

## Preconditions

- `KhaosInstance` + `KhaosPhysicalDevice` available (VK-3 complete).
- Lavapipe present; VUID gate active in-process.

## Test Cases

### Acceptance

- **TC-1: KhaosDevice construction** — Create a `KhaosDevice` from a `KhaosPhysicalDevice` with typed queue family request. Assert `VulkanOutcome.Success`. [VUID gate]
- **TC-2: QueueFamily sealed hierarchy** — `QueueFamily.Graphics`, `QueueFamily.Compute`, `QueueFamily.Transfer` all exist. A mixed-capability family is represented as a `Set<QueueFamily>` or equivalent composite — not a raw index.
- **TC-3: Typed graphics queue retrieval** — `device.graphicsQueue()` returns `KhaosQueue<Graphics>` (or equivalent typed form). Assert the returned queue is non-null and the device creation succeeded.
- **TC-4: Queue type mismatch is a compile error** — Negative compile test: a function accepting `KhaosQueue<Graphics>` must not accept `KhaosQueue<Transfer>`. Confirmed via a test-only module that attempts the wrong call and asserts compilation failure.
- **TC-5: Raw device escape hatch** — `KhaosDevice.vulkanDevice()` returns the underlying `VkDevice` reference. Assert the method exists and is marked with KDoc warning it is a last resort. Assert it is not called anywhere in the non-test codebase (grep check).
- **TC-6: Device destruction** — Destroy a `KhaosDevice`. Assert zero VUIDs on teardown. Assert subsequent calls on the destroyed device return `VulkanOutcome.Error`. [VUID gate]

### Design Contract

- **TC-7: Teardown order** — `KhaosDevice` destroyed before debug messenger, messenger before `KhaosInstance`. Assert zero VUIDs. [VUID gate — teardown order gate condition]
- **TC-8: Queue typed wrapper** — `KhaosQueue` wraps `VkQueue`; the raw `VkQueue` handle is not exposed in public API. Confirmed by grep.
- **TC-9: Queue family index not exposed** — Raw queue family index (`Int`) is not in any public `KhaosQueue` or `QueueFamily` API. The type system owns the family abstraction.

### Failure Paths

- **TC-10: Requesting unavailable queue family** — Request a queue family capability not supported by the physical device (e.g., dedicated compute on a device that only has graphics+compute combined). Assert `VulkanOutcome.Error` with a typed `UnsupportedQueueFamily` outcome — not a crash.
- **TC-11: Device creation fails** — Simulate device creation failure (e.g., request a feature not supported by Lavapipe). Assert result is `VulkanOutcome.Error`, not a thrown exception.
