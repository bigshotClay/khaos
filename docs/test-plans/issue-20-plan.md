# Test Plan — Issue #20: GRAPH-3: Pipeline state typed wrappers

## Scope

`PipelineSpec`, `KhaosPipeline`, and `DescriptorSetLayoutSpec` in `khaos-graph`. Pipeline creation requires Lavapipe on CI; data-model tests run headlessly.

## Preconditions

- `KhaosInstance`, `KhaosDevice`, render pass available (VK-3, VK-4, VK-6 complete) for live Vulkan tests.
- Lavapipe present; VUID gate active for runtime tests.

## Test Cases

### Acceptance

- **TC-1: PipelineSpec is a data class** — `PipelineSpec` is a `data class` with typed fields for all graphics pipeline state: shader stages, vertex input, input assembly, viewport/scissor, rasterization, depth/stencil, color blend. No raw `VkPipelineCreateInfo` in public API.
- **TC-2: Shader stages reference ShaderModuleHandle** — Shader stage entries in `PipelineSpec` reference `ShaderModuleHandle` values — not file paths or raw `VkShaderModule`. Confirmed by API inspection.
- **TC-3: BlendFactor and BlendOp are sealed classes** — `BlendFactor.One`, `BlendFactor.Zero`, `BlendFactor.SrcAlpha`, etc. are sealed class members. `BlendOp.Add`, etc. are sealed class members. No raw integer constants in blend state fields.
- **TC-4: DescriptorSetLayoutSpec typed bindings** — `DescriptorSetLayoutSpec` declares bindings as typed entries: binding number (`Int`), type (`DescriptorType.UniformBuffer`, etc.), and stage flags (`ShaderStage`). No raw integers.
- **TC-5: KhaosPipeline wraps pipeline + layout as unit** — `KhaosPipeline` encapsulates both `PipelineHandle` and `PipelineLayoutHandle` — they cannot be retrieved independently. Confirmed by API inspection.
- **TC-6: PipelineHandle.reusable flag** — `KhaosPipeline` creation sets `PipelineHandle(reusable = true)` for multi-draw pipelines. Assert the flag is set by the pipeline creation path, not by the caller.
- **TC-7: Triangle pipeline creation** — Create a `PipelineSpec` for the triangle shader (vertex + fragment). Create a `KhaosPipeline` from it under Lavapipe. Assert `VulkanOutcome.Success`. [VUID gate]

### Design Contract

- **TC-8: No raw VkPipeline or VkPipelineLayout in public API** — Grep assertion: no public function in `khaos-graph` exposes `PipelineHandle` and `PipelineLayoutHandle` as separate standalone values outside of `KhaosPipeline`.
- **TC-9: Shader binding type validation** — Assert that a `PipelineSpec` with a shader stage referencing bindings not present in the `DescriptorSetLayoutSpec` returns `VulkanOutcome.Error` at creation time — not at GPU dispatch. (Pre-submit validation.)
- **TC-10: Teardown** — Destroy `KhaosPipeline`. Assert zero VUIDs. Assert pipeline destroyed before `VkDevice`. [VUID gate]

### Failure Paths

- **TC-11: Shader module handle NULL** — Pass a `ShaderModuleHandle.NULL` in a shader stage. Assert `VulkanOutcome.Error` before calling `vkCreateGraphicsPipelines` — not a VUID from Vulkan.
- **TC-12: Incompatible blend op** — Specify a `BlendOp` value not supported by the device. Assert `VulkanOutcome.Error` at pipeline creation time — not a silent wrong value.
