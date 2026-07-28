# Test Plan — Issue #10: VK-6: Render pass and framebuffer typed wrappers

## Scope

Typed `KhaosRenderPass` and `KhaosFramebuffer` wrappers in `khaos-core`. All attachment, subpass, and dependency descriptions use sealed types — no raw integer flags. Requires Lavapipe on CI.

## Preconditions

- `KhaosInstance`, `KhaosPhysicalDevice`, `KhaosDevice` available (VK-3, VK-4 complete).
- Lavapipe present; VUID gate active in-process.

## Test Cases

### Acceptance

- **TC-1: RenderPassSpec construction** — Construct a `RenderPassSpec` with one color attachment (`LoadOp.Clear`, `StoreOp.Store`) and one subpass. Assert all fields are typed (no raw `Int` or `Long` in the spec).
- **TC-2: Typed LoadOp and StoreOp** — `LoadOp.Clear`, `LoadOp.Load`, `LoadOp.DontCare` are sealed class instances. `StoreOp.Store`, `StoreOp.DontCare` are sealed class instances. Confirm no raw `VkAttachmentLoadOp` integers appear in public API.
- **TC-3: Typed ImageLayout** — `ImageLayout` values (`Undefined`, `ColorAttachmentOptimal`, `PresentSrc`, etc.) are typed sealed class members. Confirm no raw `VkImageLayout` integers in public API.
- **TC-4: KhaosRenderPass creation** — Create a `KhaosRenderPass` from a `RenderPassSpec`. Assert `VulkanOutcome.Success`. [VUID gate]
- **TC-5: KhaosFramebuffer creation** — Create a `KhaosFramebuffer` from a `KhaosRenderPass` and a list of `ImageViewHandle` values matching the spec. Assert success. [VUID gate]
- **TC-6: Lifecycle — creation, use reference, destruction** — Create render pass and framebuffer; then destroy framebuffer first, then render pass. Assert zero VUIDs. Assert no handle leaks. [VUID gate]

### Design Contract

- **TC-7: SubpassDependency typed stage and access flags** — `SubpassDependency` exposes `srcStage: PipelineStage`, `dstStage: PipelineStage`, `srcAccess: AccessMask`, `dstAccess: AccessMask` as typed sealed class values — not raw `Int` bitmasks. Confirmed by API inspection.
- **TC-8: LoadOp.Clear is sealed, not enum** — `LoadOp` is a sealed class; `LoadOp.Clear` can carry a payload (clear color) in the future. Confirmed by type declaration — no `enum class LoadOp`.
- **TC-9: Attachment count validation** — Framebuffer attachment count must match render pass attachment count. Assert mismatch returns `VulkanOutcome.Error` before calling Vulkan. (Precondition check, not a VUID test.)

### Failure Paths

- **TC-10: Framebuffer destroyed before render pass** — Destroy render pass before framebuffer. Assert VUID fires (intentional VUID test — proves validation layer catches wrong destruction order). Suppress VUID for this test only with an explicit comment.
- **TC-11: Image view format mismatch** — Provide an `ImageViewHandle` whose format doesn't match the render pass attachment spec. Assert `VulkanOutcome.Error` or VUID fires at creation time. [VUID gate]
- **TC-12: Subpass with no attachments** — Construct a `RenderPassSpec` with a subpass that references no color attachments. Assert creation succeeds (valid Vulkan) or returns a typed error with explanation.
