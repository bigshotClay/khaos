# Test Plan — Issue #12: VK-8: Synchronization primitives — typed semaphores and fences

## Scope

Typed `KhaosSemaphore` and `KhaosFence` wrappers in `khaos-core`. Queue submit uses typed sync objects. Fence wait returns `VulkanOutcome`. Requires Lavapipe on CI.

## Preconditions

- `KhaosInstance`, `KhaosDevice` available (VK-3, VK-4 complete).
- Lavapipe present; VUID gate active in-process.

## Test Cases

### Acceptance

- **TC-1: KhaosSemaphore creation** — Create a `KhaosSemaphore` (binary, v0 scope). Assert creation succeeds. Assert the handle type is `SemaphoreHandle` (from VK-2). [VUID gate]
- **TC-2: KhaosFence creation** — Create a `KhaosFence` with an unsignaled initial state. Assert creation succeeds. Assert handle type is `FenceHandle`. [VUID gate]
- **TC-3: Typed queue submit** — Queue submit accepts typed `waitSemaphores: List<KhaosSemaphore>`, `signalSemaphores: List<KhaosSemaphore>`, and `fence: KhaosFence`. Assert no raw `Long` in submit API. Confirmed by API inspection.
- **TC-4: Fence wait returns VulkanOutcome** — Call `fence.wait(timeoutNs)`. Assert return is `VulkanOutcome.Success` when fence is signaled; `VulkanOutcome.Timeout` (or a named subtype) when timeout expires — not a thrown exception.
- **TC-5: Fence reset** — Call `fence.reset()`. Assert fence is unsignaled after reset. Assert `VulkanOutcome.Success`.
- **TC-6: Pipeline stage flags typed** — `PipelineStage` is a sealed class with an `or` operator for combining values. `PipelineStage.ColorAttachmentOutput or PipelineStage.Transfer` compiles and produces a combined value. No raw `Int` bitmask literals in the submit API.
- **TC-7: Frame sync pattern** — Construct a frame synchronization helper with one fence and two semaphores per frame-in-flight. Assert the helper initializes correctly for `framesInFlight = 2`. [VUID gate]
- **TC-8: Sync object lifecycle** — Create, use, and destroy semaphores and fences. Assert zero VUIDs on cleanup. Assert destruction order: sync objects destroyed before device. [VUID gate]

### Design Contract

- **TC-9: Semaphore and fence handles are distinct types** — Negative compile test: a function accepting `KhaosFence` must not accept a `KhaosSemaphore`. Confirmed via test-only module compilation failure.
- **TC-10: framesInFlight is a constructor parameter** — The frame synchronization helper's `framesInFlight` count is a constructor parameter — not a global constant. Confirmed by API inspection.

### Failure Paths

- **TC-11: Wait on unsignaled fence with zero timeout** — Call `fence.wait(0L)` on an unsignaled fence. Assert `VulkanOutcome.Timeout` is returned — not a hang or exception.
- **TC-12: Semaphore signaled twice without wait** — Signal a semaphore from a queue submit without a corresponding wait. Assert VUID fires (binary semaphore violation). [VUID gate — intentional test]
- **TC-13: Fence destroyed while signaled (before reset)** — Destroy a fence that is in a signaled state. Assert zero VUIDs (Vulkan permits this). Confirmed by VUID gate.
