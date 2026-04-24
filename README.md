# khaos

A Vulkan-first 3D rendering engine for Kotlin Multiplatform.

> **Status:** Pre-alpha — kernel in development.

## What

Khaos gives Kotlin developers a first-class path to Vulkan without C++. The core thesis: treat Vulkan's verbosity as invariant surface, not friction. Every explicit field is a place to hang a type, a test, or an assertion.

- All handles as `@JvmInline value class` — mismatched handles are compile errors
- All results as `VulkanOutcome` sealed hierarchy — no unchecked exceptions
- Render graph as pure data — compiled by a pure function, testable without a GPU
- Command recording enforced by context type — `draw()` outside a `RecordingScope` is a compile error

## Project Status

Working toward the kernel gate.

**Kernel layer progress:**

| Story | Status | Description |
|-------|--------|-------------|
| VK-1 | ✅ Done | `VulkanOutcome<T>` sealed hierarchy — exhaustive error-path typing |
| VK-2 | ✅ Done | Typed handle layer — `@JvmInline value class` for all 19 Vulkan handles |
| VK-3 | Planned | Native JNI kernel — `vkCreateInstance` through `vkCreateDevice` |
| VK-4 | Planned | Swapchain and surface management |
| VK-5 | Planned | Render pass and framebuffer layer |
| VK-6 | Planned | Pipeline and shader module layer |
| VK-7 | Planned | Command buffer recording with scope enforcement |
| VK-8 | Planned | Submit, present, synchronization |

## License

TBD
