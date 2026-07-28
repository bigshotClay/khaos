# Test Plan — Issue #6: VK-2: Typed handle layer

## Scope

Verify that every Vulkan handle used in the v0 kernel is wrapped as a distinct `@JvmInline value class`, with zero runtime overhead and no raw `Long` in public API.

## Test Cases

### Acceptance

- **TC-1: Handle set completeness** — Assert all 19 required handle types are defined: `InstanceHandle`, `DeviceHandle`, `PhysicalDeviceHandle`, `QueueHandle`, `SurfaceHandle`, `SwapchainHandle`, `ImageHandle`, `ImageViewHandle`, `RenderPassHandle`, `FramebufferHandle`, `CommandPoolHandle`, `CommandBufferHandle`, `SemaphoreHandle`, `FenceHandle`, `PipelineHandle`, `PipelineLayoutHandle`, `DescriptorSetLayoutHandle`, `BufferHandle`, `DeviceMemoryHandle`.
- **TC-2: @JvmInline annotation** — Each handle type carries `@JvmInline` annotation. Confirmed by reflection: `HandleType::class.java.annotations` contains `JvmInline`. (Or bytecode assertion: no allocation in `new HandleType(raw)`.)
- **TC-3: Zero-allocation boxing** — Verify via benchmark or bytecode inspection that constructing a handle value does not allocate a heap object. Use `@JvmInline` + `value class` on JVM: the JVM erases these to the underlying `Long` type.
- **TC-4: NULL sentinel** — Each handle type exposes a `NULL` companion constant wrapping `VK_NULL_HANDLE` (`0L`). Verify `HandleType.NULL.raw == 0L`.
- **TC-5: PipelineHandle.reusable field** — `PipelineHandle` carries a `reusable: Boolean` field. Assert it is accessible and persists through the value class wrapper.
- **TC-6: No raw Long in public API** — Grep assertion: no public function in `khaos-core` accepts or returns a raw `Long` where a handle type should appear. Gate enforced by code review + grep in CI.

### Design Contract

- **TC-7: Handle type mismatch is a compile error** — Negative compile test: a function accepting `ImageHandle` must not accept a `BufferHandle`. Confirmed by a test-only subproject that attempts the wrong-type call and asserts compilation fails (expect compile error annotation or separate module build failure).
- **TC-8: Handles are not interchangeable with Long** — Kotlin does not allow implicit widening from a value class to its backing type across API boundaries. Assert that a `Long` value cannot be passed where an `InstanceHandle` is expected without an explicit `.raw` dereference.
- **TC-9: Equality via backing value** — Two `InstanceHandle` values wrapping the same `Long` are equal (`==`). Two handles wrapping different `Long` values are not equal.

### Failure Paths

- **TC-10: NULL handle propagation** — Passing `HandleType.NULL` into a Vulkan call site (simulated) returns `VulkanOutcome.Error` or throws explicitly — not undefined behavior or a native crash. (Tested at the wrapper boundary, not with live Vulkan.)
