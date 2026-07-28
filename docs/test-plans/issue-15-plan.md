# Test Plan — Issue #15: MEM-3: Deferred deletion queue — trivially-correct single-frame implementation

## Scope

`DeferredDeletionQueue` interface and `SingleFrameDeletionQueue` implementation in `khaos-memory`. GPU-free — all tests run headlessly with mock destroyable resources.

## Test Cases

### Acceptance

- **TC-1: Interface method signatures** — `DeferredDeletionQueue` interface has `enqueue(resource: Destroyable, lifetime: ResourceLifetime)` and `drain(currentFrame: FrameIndex)`. Confirm by API inspection. No other public methods required.
- **TC-2: SingleFrameDeletionQueue enqueue + drain** — Enqueue 3 `Destroyable` mock resources. Call `drain(FrameIndex(0u))`. Assert all 3 mocks had their `destroy()` called exactly once.
- **TC-3: drain idempotency** — Enqueue 3 resources. Call `drain(FrameIndex(0u))` twice. Assert no double-destroy: each resource's `destroy()` called exactly once total.
- **TC-4: Empty drain** — Call `drain()` on an empty queue (nothing enqueued). Assert no exception, no error.
- **TC-5: Destroyable interface is typed** — Resources enqueued via `DeferredDeletionQueue` implement a `Destroyable` interface. No unchecked casts. Confirm enqueuing a non-Destroyable object causes a compile error.
- **TC-6: Interface is drop-in replaceable** — The v1 ring-buffer implementation (not yet written) would replace `SingleFrameDeletionQueue` without changing any call site — interface is the only type referenced at call sites. Confirmed by code review: no instantiation of `SingleFrameDeletionQueue` outside its factory/test.

### Design Contract

- **TC-7: SingleFrameDeletionQueue ignores FrameIndex** — In the v0 implementation, `drain(currentFrame)` destroys all enqueued resources regardless of the `currentFrame` value. Assert that `drain(FrameIndex(0u))` and `drain(FrameIndex(99u))` both destroy all queued resources.
- **TC-8: Destroy called on drain, not enqueue** — Resources are NOT destroyed at enqueue time. Assert resource `destroy()` is not called between `enqueue()` and `drain()`.
- **TC-9: Enqueue after drain** — Enqueue a resource, drain, enqueue another, drain again. Assert second resource is destroyed on the second drain — not on the first.

### Failure Paths

- **TC-10: Enqueue null-like resource** — Enqueue a `Destroyable` whose `destroy()` throws. Assert the queue handles the exception gracefully (logs it or propagates it explicitly) — does not silently swallow and skip remaining resources.
- **TC-11: Concurrent modification** — Enqueue from two coroutines simultaneously. Assert no `ConcurrentModificationException` or lost enqueue (either synchronized or documented as not thread-safe with explicit note in KDoc).
