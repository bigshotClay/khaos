# Test Plan — Issue #13: MEM-1: VMA allocator integration

## Scope

`KhaosAllocator` wrapping VMA in `khaos-memory`. All GPU allocations typed; raw `VkDeviceMemory` absent from public API. Requires Lavapipe on CI for live allocation tests.

## Preconditions

- `KhaosInstance`, `KhaosDevice` available (VK-3, VK-4 complete).
- `lwjgl-vma` on classpath.
- Lavapipe present; VUID gate active in-process.

## Test Cases

### Acceptance

- **TC-1: KhaosAllocator creation** — Create a `KhaosAllocator` from a `KhaosDevice`. Assert creation succeeds. [VUID gate]
- **TC-2: AllocatedBuffer creation** — Allocate a buffer via the allocator with `MemoryUsage.CpuToGpu` and a byte size. Assert result is `AllocatedBuffer` containing a `BufferHandle` + VMA allocation handle as an opaque unit. [VUID gate]
- **TC-3: AllocatedImage creation** — Allocate an image via the allocator with `MemoryUsage.GpuOnly`. Assert result is `AllocatedImage`. [VUID gate]
- **TC-4: AllocatedBuffer and AllocatedImage are atomic pairs** — Confirm that `AllocatedBuffer` cannot be constructed with only a `BufferHandle` (no allocation handle). Confirmed by API inspection — no public constructor splits the pair.
- **TC-5: MemoryUsage sealed class** — `MemoryUsage.GpuOnly`, `.CpuToGpu`, `.GpuToCpu`, `.CpuOnly` all exist as sealed class members. No raw `VmaMemoryUsage` integer in public API. Confirmed by inspection.
- **TC-6: Mapping and auto-unmap** — Map an `AllocatedBuffer` via `map()`. Assert result is a typed `MappedBuffer<T>`. Use it inside a `use { }` block; assert unmapping occurs on `close()` without explicit call. [VUID gate]
- **TC-7: Explicit destruction** — Call `allocatedBuffer.destroy()`. Assert both the `VkBuffer` and VMA allocation are freed atomically. Assert zero VUIDs. [VUID gate]
- **TC-8: No raw VkDeviceMemory in public API** — Grep assertion: no public function in `khaos-memory` module exposes `DeviceMemoryHandle` or equivalent raw memory handle as a standalone value (only paired within `AllocatedBuffer`/`AllocatedImage`).

### Design Contract

- **TC-9: Allocator destroyed after all allocations** — Destroy all `AllocatedBuffer` and `AllocatedImage` instances before destroying `KhaosAllocator`. Assert zero VUIDs. Assert the allocator's lifetime tracking enforces this ordering (warning or error if allocations still live at allocator destruction). [VUID gate]
- **TC-10: Write through mapped buffer** — Write data to a mapped `CpuToGpu` buffer; assert the data is visible at the GPU address (tested by reading it back in a CPU-visible mapping or by verifying via a buffer copy + readback under Lavapipe). [VUID gate]

### Failure Paths

- **TC-11: Allocation with zero size** — Attempt to allocate a buffer with size 0. Assert typed error — not a VMA assertion failure or native crash.
- **TC-12: Double-destroy** — Call `destroy()` on an `AllocatedBuffer` twice. Assert the second call returns a typed error or is a no-op — not a double-free crash.
- **TC-13: Use after destroy** — Map or access an `AllocatedBuffer` after `destroy()` has been called. Assert typed `UseAfterFree` error or documented panic — no silent undefined behavior.
