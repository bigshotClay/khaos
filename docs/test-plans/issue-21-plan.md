# Test Plan — Issue #21: GRAPH-4: Render graph execution layer

## Scope

`GraphExecutor` in `khaos-graph`. The imperative shell: records Vulkan commands from a `CompiledGraph`. Requires Lavapipe on CI. VUID + sync validation gate active.

## Preconditions

- `KhaosInstance`, `KhaosDevice`, swapchain, render pass, pipeline, command pool available (VK-3 through VK-8, GRAPH-1 through GRAPH-3 complete).
- Lavapipe present; VUID gate + synchronization validation active.

## Test Cases

### Acceptance

- **TC-1: GraphExecutor construction** — Construct a `GraphExecutor` from a `CompiledGraph`. Assert no Vulkan calls at construction time — executor is lazy until execution.
- **TC-2: Barrier recording via vkCmdPipelineBarrier2** — Execute a `CompiledGraph` with one resource transition. Inspect command recording (via `TestCommandBuffer` from TEST-2 or VUID tracing) and assert a `vkCmdPipelineBarrier2` command was recorded with values matching the `BarrierSpec`. [VUID gate]
- **TC-3: User draw lambda invoked** — Execute a `CompiledGraph` with a user-provided `context(RecordingScope)` lambda. Assert the lambda is invoked exactly once per pass. Assert the lambda receives a valid `RecordingScope`.
- **TC-4: Full frame cycle** — Execute a full frame: acquire swapchain image → record (via executor) → submit → present. Assert `VulkanOutcome.Success` at each step. Assert zero VUIDs and zero sync hazards. [VUID gate + sync validation]
- **TC-5: Typed sync objects in submit** — Frame submit uses `KhaosSemaphore` and `KhaosFence` from VK-8. Assert no raw `Long` handles in submit call. Confirmed by API inspection.
- **TC-6: SwapchainOutOfDate triggers recreation** — Simulate `SwapchainOutOfDate` on present (force via swapchain recreation path). Assert executor calls the swapchain recreation path transparently — caller does not need to handle the outcome directly.
- **TC-7: Device-agnostic CompiledGraph** — Compile a `RenderGraphSpec` on device A; execute the resulting `CompiledGraph` on device B (or a different device handle). Assert execution works without rebinding to the original compile context. (Verifies GRAPH-1/2 pure-data contract.)

### Design Contract

- **TC-8: Executor does not know draw content** — `GraphExecutor` invokes user lambdas but contains no draw call logic itself. Confirmed by code review: no `vkCmdDraw*` calls inside `GraphExecutor`.
- **TC-9: Vulkan 1.3 synchronization2 path** — Barriers use `vkCmdPipelineBarrier2` (Vulkan 1.3 core). No `vkCmdPipelineBarrier` (old API). Confirmed by grep: no `vkCmdPipelineBarrier[^2]` calls in execution path.
- **TC-10: Teardown** — Destroy executor, then all Vulkan objects in correct order. Assert zero VUIDs. [VUID gate]

### Failure Paths

- **TC-11: Execute with no passes** — Execute a `CompiledGraph` compiled from an empty `RenderGraphSpec`. Assert no crash — empty execution is a valid no-op.
- **TC-12: Draw lambda throws** — User `RecordingScope` lambda throws an exception. Assert exception propagates to the caller; no Vulkan command buffer is left in an invalid recording state. [VUID gate]
