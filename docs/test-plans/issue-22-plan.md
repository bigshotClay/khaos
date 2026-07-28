# Test Plan — Issue #22: GRAPH-5: Triangle end-to-end integration

## Scope

Full kernel integration test: GLFW window → render graph → triangle → present. All kernel components exercised together. VUID gate + sync validation + golden image gate active. This is kernel gate condition #1.

## Preconditions

- All prior issues (VK-1 through VK-8, MEM-1 through MEM-3, SHADER-1/2, GRAPH-1 through GRAPH-4) complete and green.
- Lavapipe present on CI; VUID + sync validation active.
- TEST-4 golden image infrastructure available.

## Test Cases

### Acceptance

- **TC-1: Window opens and triangle renders** — Run the end-to-end demo. Assert a GLFW window opens. Assert a colored triangle renders (visual or golden image confirmation). Assert clean exit. [VUID gate + sync validation]
- **TC-2: Hardcoded vertex buffer** — Triangle uses a hardcoded vertex buffer (positions + colors). No mesh loading. Assert the buffer is created via `KhaosAllocator` (MEM-1), not raw Vulkan. [VUID gate]
- **TC-3: Render graph spec — clear + draw + present** — The render graph has exactly: one clear pass, one draw pass, one present step. Assert the `RenderGraphSpec` structure matches. Assert `compile()` produces a `CompiledGraph` with 2 passes and barriers for the clear-to-draw and draw-to-present transitions. [VUID gate]
- **TC-4: Zero VUIDs — full lifecycle** — Run window-open, triangle-draw, and window-close. Assert zero VUIDs fire at any point. Assert sync validation reports zero hazards. [VUID gate — must be hard 0]
- **TC-5: spirv-val-validated shaders** — Vertex and fragment shaders pass `spirv-val`. Assert SHADER-1 gate is satisfied (CI `validateShaders` task is green). No invalid SPIR-V in the pipeline.
- **TC-6: Golden image passes (TEST-4)** — Render triangle to offscreen framebuffer under Lavapipe. Assert SSIM against stored golden image is above threshold. [Golden image gate — kernel gate condition]
- **TC-7: Clean exit — resource destruction** — After window close, assert all Vulkan resources are destroyed in correct order (synchronization objects, swapchain, surface, device, instance). Assert zero VUIDs on cleanup. [VUID gate]

### Design Contract

- **TC-8: RecordingScope enforced** — Draw calls inside the pass lambda are issued through a `RecordingScope` context. Assert no draw call is possible outside the lambda. Confirmed by compile-time check (VK-7 guarantee).
- **TC-9: VulkanOutcome on every step** — Every fallible kernel call (instance create, swapchain acquire, queue submit, present) returns `VulkanOutcome`. Assert no unchecked exceptions cross any of these boundaries during the triangle run.

### Failure Paths

- **TC-10: Immediate close** — Open the window and immediately request close (window-close event on first frame). Assert clean shutdown — no crash, no VUID from incomplete frame. [VUID gate]
- **TC-11: Resize / SwapchainOutOfDate** — Trigger window resize during rendering (or simulate `SwapchainOutOfDate`). Assert swapchain recreation succeeds and rendering continues. Assert zero VUIDs through the resize cycle. [VUID gate]
