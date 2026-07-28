# Test Plan — Issue #9: VK-5: Surface and swapchain typed wrappers

## Scope

Typed wrappers for `VkSurfaceKHR` and `VkSwapchainKHR` in `khaos-core`. Swapchain recreation on `SwapchainOutOfDate` is typed and tested. Requires a GLFW-capable environment or headless surface for CI.

## Preconditions

- `KhaosInstance`, `KhaosPhysicalDevice`, `KhaosDevice` available (VK-3, VK-4 complete).
- GLFW + LWJGL available; Lavapipe present for CI headless surface tests.
- VUID gate active in-process.

## Test Cases

### Acceptance

- **TC-1: Surface creation** — Create a `KhaosSurface` from a GLFW window handle. Assert creation succeeds (`VulkanOutcome.Success`). [VUID gate]
- **TC-2: Swapchain creation** — Create a `KhaosSwapchain` from a surface with typed format, extent, and present mode. Assert swapchain image handles returned as `List<ImageHandle>`. Assert no raw `Long` exposed. [VUID gate]
- **TC-3: Image handle typing** — Swapchain images returned as `List<ImageHandle>` (not `List<Long>`). Assert each element is an `ImageHandle` with a non-NULL value.
- **TC-4: Present mode fallback** — Request `PresentMode.Mailbox` with fallback to `PresentMode.Fifo`. If Lavapipe does not support Mailbox, assert Fifo is selected without error. If Mailbox is supported, assert Mailbox is selected.
- **TC-5: Swapchain recreation (no leak)** — Simulate `SwapchainOutOfDate` by calling the recreation path directly. Assert the old swapchain handle is passed as `oldSwapchain` to `vkCreateSwapchainKHR`. Assert no VUID fires (no leaked handle). [VUID gate]
- **TC-6: SwapchainOutOfDate is typed, not exception** — When `SwapchainOutOfDate` is returned, the call site handles it via a `when` branch on `VulkanOutcome` — not via a `try/catch`. Confirmed by code review.
- **TC-7: MoltenVK surface (macOS)** — On macOS CI, assert `KhaosSurface` creation uses `VK_EXT_metal_surface` or `VK_MVK_macos_surface` extension transparently. (Confirmed by build matrix test passing on macOS runner.)

### Design Contract

- **TC-8: No raw Long in swapchain API** — Grep assertion: no public function in the swapchain or surface wrappers accepts or returns a raw `Long` where a typed handle should appear.
- **TC-9: Teardown order** — Swapchain destroyed before surface, surface before device, device before instance. Assert zero VUIDs. [VUID gate]
- **TC-10: Image format type** — Swapchain `imageFormat` is a typed `ImageFormat` sealed class value — not a raw `Int` from `VkFormat`. Confirmed by API inspection.

### Failure Paths

- **TC-11: Surface format unsupported** — Request a format the surface does not support. Assert `VulkanOutcome.Error` — not a crash or silent wrong format selection.
- **TC-12: Swapchain creation on destroyed surface** — Attempt to create a swapchain after the surface has been destroyed. Assert `VulkanOutcome.Error` — not a native crash.
- **TC-13: Zero-extent swapchain** — Attempt swapchain creation with extent `(0, 0)` (minimized window case). Assert either a typed `InvalidExtent` outcome or documented handling of this edge case.
