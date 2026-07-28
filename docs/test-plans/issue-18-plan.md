# Test Plan — Issue #18: GRAPH-1: Render graph data model

## Scope

`RenderGraphSpec` and related data types in `khaos-graph`. Pure data — no GPU, no Vulkan instance. All tests run headlessly.

## Test Cases

### Acceptance

- **TC-1: RenderGraphSpec is a data class** — Assert `RenderGraphSpec` is a Kotlin `data class` (or sealed hierarchy of data classes). Confirm it has no mutable `var` fields and no GPU references. Confirmed by source inspection.
- **TC-2: Pass node construction** — Construct a `RenderPassNode` with typed input and output attachment declarations. Assert all fields are typed sealed class values — no raw `Int` or `String` keys.
- **TC-3: Resource declarations are typed** — Construct `TransientImage`, `ExternalImage` (swapchain), and `TransientBuffer` instances. Assert all are distinct types — not string-keyed maps. Assert no `String` resource identifiers in any API.
- **TC-4: Attachment access types are typed** — `ColorAttachmentWrite`, `DepthAttachmentReadWrite`, `ShaderRead` are typed sealed class members. No raw `VkAccessFlags` integers. Confirmed by API inspection.
- **TC-5: Explicit edges** — Edges between pass nodes are declared as typed resource references — not implicit ordering. Assert a spec with two passes but no edge does NOT imply ordering. Assert a spec with an explicit edge enforces the declared ordering.
- **TC-6: GPU-free construction** — Construct and compare a `RenderGraphSpec` (triangle graph: clear pass → draw pass → present) with no `VkInstance` in scope. Assert construction succeeds and no native libraries are loaded.
- **TC-7: Equality check** — Construct the same triangle spec twice. Assert `spec1 == spec2`. Mutate one field; assert `spec1 != spec2`. (Data class `equals()` covers all fields.)

### Design Contract

- **TC-8: RenderGraphSpec is copyable** — Call `.copy(...)` on a `RenderGraphSpec` and assert the copied instance is a new, equal object with the changed field. No shared mutable state between original and copy.
- **TC-9: Resource references are compile-time identifiers** — Renaming a resource reference used by two passes fails compilation (no string-based lookup that would silently mismatch). Confirmed by type system: resource references are typed identifiers, not strings.
- **TC-10: Printable (toString)** — A `RenderGraphSpec` instance `.toString()` produces a readable, non-empty string. No infinite loops or stack overflows from circular references.

### Failure Paths

- **TC-11: Pass with conflicting attachment access** — Construct a `RenderPassNode` where the same resource is declared as both `ColorAttachmentWrite` and `ShaderRead` in the same pass. Assert either a validation error at construction time or a compile-time type-system rejection — not silent corruption.
- **TC-12: Empty spec** — Construct a `RenderGraphSpec` with no passes. Assert construction succeeds (empty graph is valid data). Assert `spec.passes.isEmpty()`.
