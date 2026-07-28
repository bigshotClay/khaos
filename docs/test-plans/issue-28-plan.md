# Test Plan — Issue #28: TEST-6: Resource lifetime tests

## Scope

Destruction order, handle validity after use, and deferred deletion queue drain timing. Tests live in `khaos-memory` and `khaos-core` test sources. Most tests are GPU-free; the intentional-VUID test requires Lavapipe.

## Preconditions

- `KhaosDevice`, `KhaosCommandPool` available (VK-3, VK-4, VK-7 complete) for intentional-VUID test.
- VMA allocator available (MEM-1) for buffer-after-destroy test.
- `DeferredDeletionQueue` available (MEM-3) for queue drain tests.
- VUID gate active for all tests that touch Vulkan.

## Test Cases

### Acceptance

- **TC-1: Wrong destruction order triggers VUID** — Destroy `KhaosDevice` before its child `KhaosCommandPool`. Assert the VUID debug messenger callback fires (flag set in-process — not log scraping). Assert the VUID code matches the expected Vulkan spec VUID for parent-destroyed-before-child. **This is an intentional VUID test: explicitly suppress the VUID failure assertion for this test only, with a comment marking it as intentional-failure verification.**
- **TC-2: BufferHandle use after destroy returns typed error** — Destroy an `AllocatedBuffer`. Then call an operation on the buffer handle (e.g., map, or a wrapper method). Assert a typed error (`UseAfterFree` or equivalent) is returned — no native crash, no silent undefined behavior.
- **TC-3: DeferredDeletionQueue drain — 5 resources** — Enqueue 5 mock `Destroyable` resources. Call `drain(FrameIndex(0u))`. Assert all 5 had `destroy()` called exactly once. Assert destruction order matches enqueue order (or documented FIFO/LIFO contract).
- **TC-4: Empty drain** — Call `drain()` on an empty queue. Assert no crash, no exception, no error.
- **TC-5: Explicit pass/fail assertions** — Confirm none of the above tests use "test passes if nothing crashes" assertions. Each TC asserts a specific observable outcome (mock call count, return value, or VUID flag).
- **TC-6: GPU-free for non-VUID tests** — TC-2 through TC-5 run with no GPU, no Vulkan instance. Confirmed by test running with `VULKAN_SDK` unset.

### Design Contract

- **TC-7: VUID suppression is scoped to TC-1 only** — The intentional VUID suppression applies only within TC-1's test block. All other tests in the suite have zero-VUID requirement. Confirmed by code review: suppression is not a test-class-level setup.
- **TC-8: Destruction order test uses try/finally** — TC-1 creates a `KhaosDevice` and `KhaosCommandPool` in a try block. The intentional wrong-order destruction is in the finally block (or equivalent). All Vulkan handles are cleaned up even if the assertion fails. [Gauntlet pattern: G-02 — try/finally for all Vulkan handles]
- **TC-9: VUID callback in-process for TC-1** — TC-1 does not rely on log scraping. The VUID callback sets a boolean flag or throws in the test's scope. Confirmed by test code inspection. [Gauntlet pattern: VUID callback must be in-process]

### Failure Paths

- **TC-10: Drain a queue whose resource destroy() throws** — Enqueue a `Destroyable` whose `destroy()` throws `RuntimeException`. Assert the queue does NOT silently swallow the exception and skip remaining resources. Remaining resources are still destroyed; the exception is either propagated or collected and re-thrown after all resources are processed.
- **TC-11: Double-drain** — Enqueue 3 resources; drain twice. Assert no double-destroy (each resource destroyed exactly once). Assert second drain is a safe no-op.
